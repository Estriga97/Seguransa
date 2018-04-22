package managers;

import sun.misc.BASE64Encoder;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

import static java.util.Base64.*;

public class manUsers {


    private static File fl = new File("Files");
    private static File passFile = new File(fl, "users.txt");
    private static File macFile = new File(fl, "sec.mac");


    public static void main (String[] args) {

        try {

            //criação da pasta
            fl.mkdir();


            //autenticação do admin
            Scanner scanner = new Scanner(System.in);
            System.out.println("Administrator, what's the passord?");
            String password = scanner.next();

            //o nosso sal, cheio de amor
            byte[] salt = {0x6e, 0x69, 0x73, 0x63, 0x6f, 0x6e, 0x69, 0x69, 0x2d, 0x63, 0x68, 0x61, 0x6e, 0x20, 0x62, 0x72, 0x75, 0x6e, 0x6f, 0x6e, 0x69, 0x69, 0x2d, 0x63, 0x68, 0x61, 0x6e, 0x20, 0x64, 0x61, 0x69, 0x73, 0x75, 0x6b, 0x69};

            //codificação de pass do admin
            PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 20);
            SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
            SecretKey key = kf.generateSecret(keySpec);


            //verificar se é a primeira execução
            fl.isDirectory();
            if(fl.list().length > 0) {

                FileInputStream fos = new FileInputStream(macFile);
                ObjectInputStream oos = new ObjectInputStream(fos);

                //verificar se ficheiro das pass's n foi alterado
                byte[] oldMac = (byte[]) oos.readObject();
                oos.close();
                if(!Arrays.equals(makeMac(key), oldMac)) {
                    System.out.println("Access Denied: Wrong Password or Corrumpted File");
                    System.exit(-1);

                }
            }
            else {
                //criação dos ficheiros
                passFile.createNewFile();
                macFile.createNewFile();

                FileOutputStream fos = new FileOutputStream(macFile);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(makeMac(key));
                oos.close();
            }

            //pedido do comando
            System.out.println("Administrator, what's the command?");
            System.out.println("Available Commands: add, delete, modify & quit");
            String command = scanner.next();

            while(true) {
                processCommand(command, scanner, key);
                System.out.println("Operation successful");
                System.out.println("What's next the command?");
                command = scanner.next();
            }


        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


    }



    /////////////////////////////////////// AUXILIARY METHODS  ////////////////////////////////////////////////////////////////////////////


    private static byte[] makeMac(SecretKey k) throws NoSuchAlgorithmException, InvalidKeyException, IOException {

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(k);
        byte[] bt = Files.readAllBytes(passFile.toPath());
        return mac.doFinal(bt);
    }

    private static void processCommand(String comand, Scanner scanner, SecretKey key) throws IOException, InvalidKeyException, NoSuchAlgorithmException {


        //inicio do processamento

        Base64.Encoder enc = getEncoder();
        String name;
        String pass;
        byte[] salt;
        String userInfo;
        byte[] salted;



        //processamento do comand
        switch (comand) {

            case "add":

                BufferedWriter bw = new BufferedWriter(new FileWriter(passFile, true));

                FileOutputStream fos = new FileOutputStream(macFile);
                ObjectOutputStream oos = new ObjectOutputStream(fos);

                System.out.println("Adding new User:");

                System.out.println("What's the username?");
                name = scanner.next();

                System.out.println("What's the password?");
                pass = scanner.next();

                salt = makeSalt();

                salted = getSalty(pass, salt);

                bw.write(name + ":" + enc.encodeToString(salt) + ":" + enc.encodeToString(salted) + "\n");
                bw.close();

                oos.writeObject(makeMac(key));
                oos.close();


                break;

            case "delete":

                System.out.println("Deleting User:");

                //verificação de name
                System.out.println("What's the username?");
                name = scanner.next();
                userInfo = searchUser(name);

                if(userInfo == null) {
                    System.out.println("Error: User not found");
                    break;
                }

                deleteUser(name);

                FileOutputStream fos3 = new FileOutputStream(macFile);
                ObjectOutputStream oos3 = new ObjectOutputStream(fos3);
                oos3.writeObject(makeMac(key));
                oos3.close();

                break;

            case "modify":

                System.out.println("Modifying User:");

                //verificação de name
                System.out.println("What's the username?");
                name = scanner.next();
                userInfo = searchUser(name);

                if(userInfo == null) {
                    System.out.println("Error: User not found");

                    break;
                }

                //verificação de pass
                salt = makeSalt();

                //defenição de nova pass
                System.out.println("What's the new password?");
                pass = scanner.next();

                salted = getSalty(pass, salt);
                modifyUser(name, enc.encodeToString(salt), enc.encodeToString(salted));

                FileOutputStream fos2 = new FileOutputStream(macFile);
                ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
                oos2.writeObject(makeMac(key));
                oos2.close();

                break;

            case "quit":

                System.out.println("Program terminated!");
                System.exit(0);

                break;

        }
    }



    //vós sois o sal da terra
    private  static  byte[] makeSalt() {

        final Random r = new SecureRandom();
        byte[] salt = new byte[64];
        r.nextBytes(salt);
        return salt;

    }


    private static byte[] getSalty(String pass, byte[] salt) throws NoSuchAlgorithmException, IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(pass.getBytes());
        bos.write(salt);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(bos.toByteArray());

    }

    private static String searchUser(String name) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(passFile));
        String line = br.readLine();
        while(!name.equals(line.split(":")[0])) {
            line = br.readLine();
        }

        br.close();

        return line;
    }

    private static boolean deleteUser(String name) throws IOException {


        BufferedReader br = new BufferedReader(new FileReader(passFile));
        File help = new File(fl, "help.txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(help));

        String line = br.readLine();
        System.out.println(name);
        while(line != null) {
            System.out.println(line.split(":")[0]);
            if(!name.equals(line.split(":")[0])) {
                System.out.println("ca dentro:" + line.split(":")[0]);
                bw.write(line + "\n");
            }
            line = br.readLine();
        }

        br.close();
        bw.close();

        return help.renameTo(passFile);

    }

    private static boolean modifyUser(String name, String salt, String salted) throws IOException {


        BufferedReader br = new BufferedReader(new FileReader(passFile));
        File help = new File(fl, "help.txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(help));

        FileOutputStream fos = new FileOutputStream(macFile);
        ObjectOutputStream oos = new ObjectOutputStream(fos);

        String line = br.readLine();
        while(line != null) {
            if(!name.equals(line.split(":")[0]))
                bw.write(line + "\n");
            else
                bw.write(name + ":" + salt + ":" + salted + "\n");
            line = br.readLine();
        }

        br.close();
        bw.close();

        return help.renameTo(passFile);

    }



}

/*

duvidas:
como defenir qual a pass do admin -- n se define
como verificar se o ficheiro das passes esta ok -- done


*/
