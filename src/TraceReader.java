import java.io.*;
import java.math.BigInteger;

/**
 * Created by woojin on 2016. 6. 7..
 */
public class TraceReader {
    public static void read(String filename, ITraceProcceable cb) {
        if (filename.length() < 1) {
            throw new Error("Filename should be specified");
        }

        int READ_CHUNK = 10000;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String s;
            int cnt = 1;
            int chuckCount = 1;
            while ((s = reader.readLine()) != null) {
                if (cnt % READ_CHUNK == 0) {
                    System.out.printf("%d lines read... - %d\n", READ_CHUNK, chuckCount);
                    cnt = 0;
                    chuckCount++;
                }

                String[] traceComponents = s.split(" ");
                if (traceComponents.length < 2) {
                    return;
                }
                Trace tr = new Trace();
                tr.accessType = Integer.parseInt(traceComponents[0], 10);
                tr.address = new BigInteger(getHexStringWithoutPrefix(traceComponents[1]), 16);

                cb.handleTrace(tr);

                cnt ++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private static String getHexStringWithoutPrefix(String hex) {
        return hex.startsWith("0x") ? hex.substring(2) : hex;
    }
}

class Trace {
    static final int ACCESS_TYPE_DATA_READ = 0;
    static final int ACCESS_TYPE_DATA_WRITE = 1;
    static final int ACCESS_TYPE_INSTRUCTION_READ = 2;
    static final int ACCESS_TYPE_FILE_WRITE = 3;

    int accessType;
    BigInteger address;
}

interface ITraceProcceable {
    void handleTrace(Trace tr);
}
