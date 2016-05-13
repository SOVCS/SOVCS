package com.SOVCS.StateMachine;

import io.atomix.catalyst.transport.Address;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by eric on 5/8/16.
 */
public class FileState implements Serializable {
    private File name; //file name including relative path
    private String checksum; //file checksum
    private int key; //hashed key value
    private transient Address address;
    private byte[] bytes;

    public FileState(File fileName, Address address) {
        this.name = fileName;
        try {
            this.checksum = checksum(fileName);
        } catch (IOException e) {
            System.out.println("Unable to open file");
            System.exit(1);
        } catch (NoSuchAlgorithmException a){
            System.out.println("Message Digest is using an incorrect algorithm");
            System.exit(1);
        }
        this.key = fileName.hashCode();
        //The static address will be passed in here
        this.address = address;
    }
    public int getKey(){
        return this.key;
    }

    public File getFile(){
        return this.name;
    }

    public String getChecksum(){
        return this.checksum;
    }

    public Address getAddress(){
        return this.address;
    }

    public byte[] getBytes(){
        return this.bytes;
    }

    public void setBytes(byte[] bytes) { this.bytes = bytes; }

    public void newChecksum(){
        try {
            this.checksum = this.checksum(this.name);
        } catch (IOException e) {
            System.out.println("Unable to open file");
            System.exit(1);
        } catch (NoSuchAlgorithmException a){
            System.out.println("Message Digest is using an incorrect algorithm");
            System.exit(1);
        }

    }
    private String checksum(File file) throws IOException, NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA1");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[1024];
        int k = 0;
        while ((k = fis.read(data)) != -1) {
            digest.update(data, 0, k);
        }

        byte[] msize = digest.digest();

        //convert the byte to hex format
        StringBuffer sb = new StringBuffer("");
        for (int i = 0; i < msize.length; i++) {
            sb.append(Integer.toString((msize[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();

    }
}