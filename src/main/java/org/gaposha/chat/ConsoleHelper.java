package org.gaposha.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {
    private static BufferedReader bufferedReader
            = new BufferedReader(new InputStreamReader(System.in));


    public static void writeMessage(String message) {
        System.out.println(message);
    }

    public static String readString() {
        while (true) {
            try {
                String string = bufferedReader.readLine();
                if (string != null) {
                    return string;
                }
            } catch (IOException ioException) {
                System.out.println("Произошла ошибка при попытке ввода текста. Попробуйте еще раз.");
                readString();
            }
        }
    }

    public static int readInt() {
        while (true) {
            try {
                return Integer.parseInt(readString());
            } catch (NumberFormatException numberFormatException) {
                System.out.println("Произошла ошибка при попытке ввода числа. Попробуйте еще раз.");
                readInt();
            }
        }
    }
}
