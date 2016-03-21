package ru.ncapital.gateways.micexfast.connection.tcp;

import org.openfast.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by egore on 1/20/16.
 */
public class TcpConnectionManager {

    private TcpConnection connection;

    private void addLengthAndCheckSum(ByteBuffer bb, String body) {
        int length = body.length();

        StringBuilder sb = new StringBuilder();
        sb.append(8).append('=').append("FIXT1.1").append((char) 0x01);
        sb.append(9).append('=').append(length).append((char) 0x01);
        sb.append(body);

        bb.put(sb.toString().getBytes());

        int checkSum = 0;
        for (int i = 0; i < bb.position(); ++i) {
            checkSum += bb.get(i);
        }
        checkSum %= 256;

        bb.put(String.format("10=%03d", checkSum).getBytes()).put((byte) 0x01);
    }

    private ByteBuffer getLoginMessage(String username, String password, String senderCompId) {
        ByteBuffer bb = ByteBuffer.allocate(1400);
        StringBuilder sb = new StringBuilder();

        sb.append(35).append('=').append("A").append((char) 0x01);
        sb.append(49).append('=').append(senderCompId).append((char) 0x01);
        sb.append(56).append('=').append("MOEX").append((char) 0x01);
        sb.append(34).append('=').append("1").append((char) 0x01);
        sb.append(52).append('=').append(System.currentTimeMillis()).append((char) 0x01);
        sb.append(553).append('=').append(username).append((char) 0x01);
        sb.append(554).append('=').append(password).append((char) 0x01);
        sb.append(1137).append('=').append("9").append((char) 0x01);

        addLengthAndCheckSum(bb, sb.toString());

        bb.flip();
        return bb;
    }

    public Message[] recoverMessages(int beginSeqNo, int endSeqNo) throws IOException {
        // open TCP connection
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(connection.getHost(), connection.getPort()));

        // login
        ByteBuffer loginRequest = getLoginMessage("username", "password", "111");
        channel.write(loginRequest);

        // receive login response
        ByteBuffer loginResponse = ByteBuffer.allocate(1400);
        channel.read(loginResponse);

        // decode login response
        // ...

        // request messages

        // receive messages

        // logout

        // close TCP connection

        return null;
    }
}
