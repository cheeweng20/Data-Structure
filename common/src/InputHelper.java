package common.src;

import java.util.InputMismatchException;
import java.util.Scanner;

public class InputHelper {

    public static int inputInt(Scanner input, String prompt) {
        while (true) {
            try{
                System.out.print(ConsoleStyle.prompt(prompt));
                int userInput = input.nextInt();
                return userInput;
            }catch(InputMismatchException ex){
                System.out.println(ConsoleStyle.error("Invalid Input, Please Try Again !"));
                input.nextLine();
            }
        }
    }

    public static String inputString(Scanner input,String prompt){
        System.out.print(ConsoleStyle.prompt(prompt));
        String userInput = input.nextLine();
        return userInput;
    }
}
