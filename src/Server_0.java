import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.*;

public class Server_0 {
    public static void main(String[] args) throws IOException {
        Server_0 server = new Server_0();
        server.boot();
    }

    private void boot() throws IOException {
        serverSocket = new ServerSocket(8000);
        Socket socket = serverSocket.accept();
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        int oneInt = -1;
        byte oldByte = (byte)-1;
        StringBuilder sb = new StringBuilder();
        int lineNumber = 0;
        boolean bodyFlag = false;
        String method = null;
        String requestUrl = null;
        String httpVersion = null;
        int contentLength = -1;
        int bodyRead = 0;
        List<Byte> bodyByteList = null;
        Map<String, String> headerMap = new HashMap<String, String>();
        while (-1 != (oneInt = in.read())) {
            byte thisByte = (byte)oneInt;
            if (bodyFlag) {
                bodyRead++;
                bodyByteList.add(thisByte);
                if (bodyRead >= contentLength) {
                    break;
                }
            }
            else {
                if (thisByte == Server_0.LF && oldByte == Server_0.CR) {
                    // CRLF가 완성되었음 따라서 직전 CRLF부터 여기까지가 한 행임
                    // substring -2가 아닌 -1을 하는 이유는 아직 LF가 버퍼에 들어가기 전이기 때문이다.
                    String oneLine = sb.substring(0, sb.length() - 1);
                    lineNumber++;

                    if (lineNumber == 1) {
                        // 요청의 첫 행, HTTP 메서드, 요청 URL, 버전을 알아냄
                        int firstBlank = oneLine.indexOf(" ");
                        int secondBlank = oneLine.lastIndexOf(" ");
                        method = oneLine.substring(0, firstBlank);
                        requestUrl = oneLine.substring(firstBlank + 1, secondBlank);
                        httpVersion = oneLine.substring(secondBlank + 1);
                    }
                    else {
                        if (oneLine.length() <= 0) {
                            // 내용이 없는 행
                            // 따라서 메시지 헤더의 마지막일 경우임
                            bodyFlag = true;
                            if ("GET".equals(method)) {
                                // GET 방식이면 메시지 바디가 없음
                                break;
                            }
                            String contentLengthValue = headerMap.get("Content-Length");
                            if (contentLengthValue != null) {
                                contentLength = Integer.parseInt(contentLengthValue.trim());
                                bodyFlag = true;
                                bodyByteList = new ArrayList<Byte>();
                            }
                            continue;
                        }
                        int indexOfColon = oneLine.indexOf(":");
                        String headerName = oneLine.substring(0, indexOfColon);
                        String headerValue = oneLine.substring(indexOfColon + 1);
                        headerMap.put(headerName, headerValue);
                    }
                    sb.setLength(0);
                }
                else {
                    sb.append((char)thisByte);
                }
            }
            oldByte = (byte)oneInt;
        }
        out.close();
        in.close();
        socket.close();

        System.out.printf("METHOD: %s REQ: %s HTTP VER. %s\n", method, requestUrl, httpVersion);

        Map<String, String> paramMap = new HashMap<String, String>();
        int indexOfQuotation = requestUrl.indexOf("?");
        if (indexOfQuotation > 0) {
            StringTokenizer st = new StringTokenizer(requestUrl.substring(indexOfQuotation+1), "&");

            while (st.hasMoreTokens()) {
                String params = st.nextToken();
                paramMap.put(params.substring(0, params.indexOf("=")), params.substring(params.indexOf("=")+1));
            }
        }

        System.out.println("Header list");
        Set<String> keySet = headerMap.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String headerName = keyIter.next();
            System.out.printf(" Key: %s Value: %s\n", headerName, headerMap.get(headerName));
        }
        if (bodyByteList != null) {
            if ("application/x-www-form-urlencoded".equals(headerMap.get("Content-Type").trim())) {
                int startIndex = 0;
                byte[] srcBytes = new byte[bodyByteList.size()];
                String currentName = null;
                for (int i=0; i<bodyByteList.size(); i++) {
                    byte oneByte = bodyByteList.get(i);
                    srcBytes[i] = oneByte;
                    if ('=' == oneByte) {
                        byte[] one = new byte[i - startIndex];
                        System.arraycopy(srcBytes, startIndex, one, 0, i - startIndex);
                        currentName = URLDecoder.decode(new String(one), "CP949");
                        startIndex = i + 1;
                    }
                    else if ('&' == oneByte) {
                        byte[] one = new byte[i - startIndex];
                        System.arraycopy(srcBytes, startIndex, one, 0, i - startIndex);
                        paramMap.put(currentName, URLDecoder.decode(new String(one), "CP949"));
                        startIndex = i + 1;
                    }
                    else if (i == bodyByteList.size() - 1) {
                        byte[] one = new byte[i - startIndex + 1];
                        System.arraycopy(srcBytes, startIndex, one, 0, i - startIndex + 1);
                        paramMap.put(currentName, URLDecoder.decode(new String(one), "CP949"));
                        startIndex = i + 1;
                    }
                }
            }
            else {
                System.out.print("Message Body-->");
                for(byte oneByte : bodyByteList) {
                    System.out.print(oneByte);
                }
                System.out.println("<--");
            }
        }

        Set<String> paramKeySet = paramMap.keySet();
        Iterator<String> paramKeyIter = paramKeySet.iterator();
        while (paramKeyIter.hasNext()) {
            String paramName = paramKeyIter.next();
            System.out.printf("paramName: %s paramValue: %s\n", paramName, paramMap.get(paramName));
        }
        System.out.println("End of HTTP Message.");
    }

    public static final byte CR = '\r';
    public static final byte LF = '\n';
    private ServerSocket serverSocket;
}
