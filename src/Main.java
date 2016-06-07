/**
 * Created by woojin on 2016. 6. 7..
 */
public class Main {
    public static void main(String args[]) {
        final TraceInfo info = new TraceInfo(10);

        TraceReader.read("res/trace-redis.txt", new ITraceProcceable() {
            @Override
            public void handleTrace(Trace tr) {
                // 파일 읽은 한 줄 할 줄이 Trace 클래스로 담겨서 전달됨.
                info.inputTrace(tr);
            }
        });

        System.out.printf("%d, %d, %d / %d", info.getReadChunkCount(), info.getWriteChunkCount(), info.getInstReadChunkCount(), info.getTotalCount());
    }
}

class TraceInfo {
    private int chunkCriteria;

    private int totalCount = 0;
    private int readChunkCount = 0;
    private int instReadChunkCount = 0;
    private int writeChunkCount = 0;

    private int previousAccessType = -1;
    private int subsequentAccessCount = 0;

    TraceInfo(int chuckCriteria) {
        if (chuckCriteria < 1) {
            // default
            this.chunkCriteria = 10;
        }

        this.chunkCriteria = chuckCriteria;
    }

    public void inputTrace(Trace tr) {
        if (previousAccessType == tr.accessType) {
            subsequentAccessCount++;
        } else {
            if (subsequentAccessCount > chunkCriteria) {
                if (previousAccessType == Trace.ACCESS_TYPE_DATA_READ) {
                    readChunkCount++;
                } else if (previousAccessType == Trace.ACCESS_TYPE_DATA_WRITE || previousAccessType == Trace.ACCESS_TYPE_FILE_WRITE) {
                    writeChunkCount++;
                } else {
                    instReadChunkCount++;
                }
            }
            subsequentAccessCount = 0;
        }

        previousAccessType = tr.accessType;
        totalCount++;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getReadChunkCount() {
        return readChunkCount;
    }

    public int getInstReadChunkCount() {
        return instReadChunkCount;
    }

    public int getWriteChunkCount() {
        return writeChunkCount;
    }
}