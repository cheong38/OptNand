import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by woojin on 2016. 6. 7..
 */
public class CacheMedia {
    final static int WORD_SIZE = 8;  // 64 bit 시스템에서는 8바이트 씩 이동

    final static int KIND_INSTRUCTION = 1;
    final static int KIND_DATA = 2;
    final static int KIND_UNIFIED = 3;

    private int capacity;
    private int readTime;
    private int writeTime;
    private int kind;

    private boolean isNeededToWriteBack;

    private ArrayList<CacheLine> cacheLines = new ArrayList<>();
    private int top = 0;
    private int numberOfCacheLines;

    CacheMedia(int capacity, int readTime, int writeTime, int kind) {
        this.capacity = capacity;

        numberOfCacheLines = capacity / CacheLine.SIZE;

        for (int i = 0; i < numberOfCacheLines; i++) {
            cacheLines.add(new CacheLine(null));
        }

        this.readTime = readTime;
        this.writeTime = writeTime;
        this.kind = kind;
    }

    public boolean access(int accessType, BigInteger address) {
        boolean isHit = false;

        int index = findIndexOfCacheLine(address);

        if (index != -1) {
            // address 가 cacheLines 안에 있으면 hit
            // hit 이면 true 반환

            isHit = true;

            CacheLine cacheLine = cacheLines.get(index);

            // 쓰기 연산이면 dirty 를 set 함
            if (accessType == Trace.ACCESS_TYPE_DATA_WRITE || accessType == Trace.ACCESS_TYPE_FILE_WRITE) {
                cacheLine.setDirty(true);
            }

            // Hit 시 최근 사용한 데이터이므로 마지막으로 보냄
            BigInteger temp = new BigInteger(cacheLine.getAddress().toString());
            cacheLine.address = cacheLines.get(top - 1).getAddress();
            cacheLines.get(top - 1).address = temp;

        } else {
            // address 가 cacheLines 안에 없으면 miss
            // miss 면 cache line replace 하고 false 반환
            // replace 를 하기 위해 eviction 할 캐시 라인이 dirty 이면 write back 수행
            isHit = false;

            int indexToUpdate = getIndexToUpdate();
            CacheLine cacheLineToEvict = cacheLines.get(indexToUpdate);
            isNeededToWriteBack = cacheLineToEvict.getDirty();
            cacheLineToEvict.address = address;
            cacheLineToEvict.setDirty(false);
        }

        return isHit;
    }

    public void updateCacheLinesForPage(BigInteger address, long pageSize) {
        // address 를 page 크기로 나누면 page 번호
        long pageNumber = address.divide(new BigInteger(String.valueOf(pageSize))).longValue();

        BigInteger startAddressOfThePage = new BigInteger(String.valueOf(pageNumber * pageSize));
        for (long i = 0; i < pageSize / WORD_SIZE; i++) {
            BigInteger addressToUpdate = startAddressOfThePage.multiply(new BigInteger(String.valueOf(i * WORD_SIZE)));
            int indexToUpdate = getIndexToUpdate();
            if (indexToUpdate == 0) {
                // index 가 0 이면 앞부분 전체를 남은 부분 만큼 업데이트
                for (long j = i; j < pageSize / WORD_SIZE; j++) {
                    addressToUpdate = startAddressOfThePage.multiply(new BigInteger(String.valueOf(j * WORD_SIZE)));
                    cacheLines.get(indexToUpdate).address = addressToUpdate;
                    return;
                }
            } else {
                cacheLines.get(indexToUpdate).address = addressToUpdate;
            }
        }
    }

    private int getIndexToUpdate() {
        // 아직 cache 사이즈를 다 채우지 못했으면, 데이터가 없는 가장 앞 번호의 인덱스
        // cache 사이즈가 가득 찼으면, 첫 번째 인덱스
        // 첫 번째 인덱스가 가장 오래 전에 사용했던 인덱스
        return top < numberOfCacheLines ? top++ : 0;
    }

    private int findIndexOfCacheLine(BigInteger address) {
        // 해당 하는 캐시 라인이 없으면 -1,
        // 해당 하는 캐시 라인이 있으면 해당 인덱스
        for (int i = 0; i < cacheLines.size(); i++) {
            if (cacheLines.get(i).address == null) {
                return -1;
            } else if (cacheLines.get(i).address.equals(address)) {
                return i;
            }
        }

        return -1;
    }

    public boolean isNeededToWriteBack() {
        return isNeededToWriteBack;
    }

    public void completeWriteBack() {
        isNeededToWriteBack = false;
    }

    public int getReadTime() {
        return readTime;
    }

    public int getWriteTime() {
        return writeTime;
    }

    private class CacheLine {
        private BigInteger address;
        private boolean isDirty = false;
        final static int SIZE = 64;  // 64바이트로 고정

        CacheLine(BigInteger address) {
            this.address = address;
        }

        public BigInteger getAddress() {
            return address;
        }

        public void setAddress(BigInteger address) {
            this.address = address;
        }

        public void setDirty(boolean dirty) {
            isDirty = dirty;
        }

        public boolean getDirty() {
            return isDirty;
        }
    }
}