package common.src;

import java.util.InputMismatchException;
import java.util.Scanner;

public class InputHelper {

    public static int inputInt(Scanner input, String prompt) {
        while (true) {
            try{
                System.out.print(prompt);
                int userInput = input.nextInt();
                return userInput;
            }catch(InputMismatchException ex){
                System.out.println("Invalid Input, Please Try Again !");
                input.nextLine();
            }
        }
    }

    public static String inputString(Scanner input,String prompt){
        System.out.print(prompt);
        String userInput = input.nextLine();
        return userInput;
    }
}