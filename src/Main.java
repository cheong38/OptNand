/**
 * Created by woojin on 2016. 6. 7..
 */
public class Main {
    public static void main(String args[]) {
        final int KB = (int) Math.pow(2, 10);
        final int MB = (int) Math.pow(2, 20);
        final int PAGE_SIZE = 4 * KB;
        final SimulateInfo simulateInfo = new SimulateInfo();

        final CacheMedia L1Data = new CacheMedia(32 * KB, 2, 2, CacheMedia.KIND_DATA);
        final CacheMedia L1Instruction = new CacheMedia(32 * KB, 2, 2, CacheMedia.KIND_INSTRUCTION);
        final CacheMedia L2 = new CacheMedia(512 * KB, 30, 30, CacheMedia.KIND_UNIFIED);
        final CacheMedia DRAM = new CacheMedia(40 * MB, 210, 210, CacheMedia.KIND_UNIFIED);

        TraceReader.read("res/trace-redis2-sample.txt", new ITraceProcceable() {
            @Override
            public void handleTrace(Trace tr) {
                // 파일 읽은 한 줄 할 줄이 Trace 클래스로 담겨서 전달됨.
                CacheMedia L1;
                if (tr.accessType == Trace.ACCESS_TYPE_INSTRUCTION_READ) {
                    L1 = L1Instruction;
                } else {
                    L1 = L1Data;
                }

                boolean isL1Hit = L1.access(tr.accessType, tr.address);

                if (isL1Hit) {
                    // L1 캐시의 액세스 타임 더해줌줌
                    if (tr.accessType == Trace.ACCESS_TYPE_INSTRUCTION_READ || tr.accessType == Trace.ACCESS_TYPE_DATA_READ) {
                        simulateInfo.addAccessTime(L1.getReadTime());
                    } else {
                        simulateInfo.addAccessTime(L1.getWriteTime());
                    }

                    simulateInfo.hitL1();

                } else {
                    // write type 연산이면 하위 메모리에 접근할 필요가 없음
                    if (L1.isNeededToWriteBack()) {
                        //  write back 할 동안 L2 write 시간만큼 기다려야 함
                        simulateInfo.addAccessTime(L2.getWriteTime());
                        L1.completeWriteBack();

                        // L1 write back 이면 L2 hit 으로 산정정
                        simulateInfo.hitL2();
                    } else {
                        // 만약 accessType이 read이면
                        // L2에 access
                        boolean isL2Hit = L2.access(tr.accessType, tr.address);
                        if (isL2Hit) {
                            simulateInfo.addAccessTime(L2.getReadTime());
                            simulateInfo.hitL2();
                        } else {
                            if (L2.isNeededToWriteBack()) {
                                simulateInfo.addAccessTime(DRAM.getWriteTime());
                                L2.completeWriteBack();

                                // L2 에서 write back이면 DRAM hit으로 산정
                                simulateInfo.hitDram();
                            }

                            boolean isDramHit = DRAM.access(tr.accessType, tr.address);

                            if (isDramHit) {
                                simulateInfo.addAccessTime(DRAM.getReadTime());
                                simulateInfo.hitDram();
                            } else {
                                if (DRAM.isNeededToWriteBack()) {
                                    // NAND flash의 write time = 200000 + 200
                                    simulateInfo.addAccessTime(200200);
                                } else {
                                    // NANA flash의 read time = 25000 + 200
                                    simulateInfo.addAccessTime(25200);

                                    // DRAM에 데이터가 없는 경우, DRAM에 데이터를 page 단위로 업데이트
                                    DRAM.updateCacheLinesForPage(tr.address, PAGE_SIZE);
                                    simulateInfo.addUpdatePageCount();
                                }

                                // NAND에는 무조건 데이터가 있다고 가정
                                simulateInfo.hitNand();
                            }

                        }
                    }

                }

                simulateInfo.increaseAccessCount();
            }
        });

        System.out.printf("Total Access Time: %d\nTotal Access Count: %d\nL1 Hit Count: %d\nL2 Hit Count: %d\nDram Hit Count: %d\nNAND Hit Count: %d\n",
                simulateInfo.getTotalAccessTime(), simulateInfo.getTotalAccessCount(),
                simulateInfo.getL1HitCount(), simulateInfo.getL2HitCount(),
                simulateInfo.getDramHitCount(), simulateInfo.getNandHitCount());
    }
}

class SimulateInfo {
    private long totalAccessCount = 0;
    private long totalAccessTime = 0;
    private long L1HitCount = 0;
    private long L2HitCount = 0;
    private long DramHitCount = 0;
    private long NandHitCount = 0;
    private long updatePageCount = 0;

    public void addUpdatePageCount() {
        updatePageCount++;
    }

    public long getUpdatePageCount() {
        return updatePageCount;
    }

    public void addAccessTime(long accessTime) {
        totalAccessTime += accessTime;
    }

    public long getTotalAccessTime() {
        return totalAccessTime;
    }

    public void hitL1() {
        L1HitCount += 1;
    }

    public long getL1HitCount() {
        return L1HitCount;
    }

    public void hitL2() {
        L2HitCount += 1;
    }

    public long getL2HitCount() {
        return L2HitCount;
    }

    public void hitDram() {
        DramHitCount += 1;
    }

    public long getDramHitCount() {
        return DramHitCount;
    }

    public void hitNand() {
        NandHitCount += 1;
    }

    public long getNandHitCount() {
        return NandHitCount;
    }

    public void increaseAccessCount() {
        totalAccessCount += 1;
    }

    public long getTotalAccessCount() {
        return totalAccessCount;
    }
}