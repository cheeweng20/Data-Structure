import java.util.Scanner;

/** Application entry point for the unified hotel management system. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        new MainUI(new Scanner(System.in)).start();
    }
}
