package org.gaposha.chat.client;

import org.gaposha.chat.Connection;
import org.gaposha.chat.ConsoleHelper;
import org.gaposha.chat.message.Message;
import org.gaposha.chat.message.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();

    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        return ConsoleHelper.readInt();
    }

    protected  String getUserName() {
        ConsoleHelper.writeMessage("Введите ник пользователя:");
        return ConsoleHelper.readString();
    }

    protected  boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException ioException) {
            ConsoleHelper.writeMessage("Произошла ошибка при отправке сообщения.");
            clientConnected = false;
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException interruptedException) {
                ConsoleHelper.writeMessage("Возникла ошибка!");
                return;
            }
        }

        if(clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }

        while (clientConnected) {
            String message = ConsoleHelper.readString();
            if(message.equalsIgnoreCase("exit")) break;
            if(shouldSendTextFromConsole()) {
                sendTextMessage(message);
            }
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public class SocketThread extends Thread {

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("Пользователь %s присоединился к чату.", userName));
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage(String.format("Пользователь %s покинул чат.", userName));

        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
               Message message = connection.receive();
               if(message.getType() == MessageType.NAME_REQUEST) {
                   Message userName = new Message(MessageType.USER_NAME, getUserName());
                   connection.send(userName);
               } else if (message.getType() == MessageType.NAME_ACCEPTED) {
                   notifyConnectionStatusChanged(true);
                   break;
               } else {
                   throw new IOException("Unexpected MessageType");
               }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if(message.getType() == MessageType.TEXT) {
                    processIncomingMessage(message.getData());
                } else if (message.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(message.getData());
                } else if (message.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        @Override
        public void run() {
            String host = getServerAddress();
            int port = getServerPort();
            try {
                Socket clientSocket = new Socket(host, port);
                Client.this.connection = new Connection(clientSocket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException exception) {
                notifyConnectionStatusChanged(false);
            }
        }
    }
}
