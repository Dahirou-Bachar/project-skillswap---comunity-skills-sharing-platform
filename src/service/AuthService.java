package service;

import java.io.BufferedReader;
import java.io.FileReader;

public class AuthService {

    public static boolean login(String username, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader("users.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    if (parts[0].equals(username) && parts[1].equals(password)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading users.txt");
        }
        return false;
    }
}
