package com.SOVCS.main;

import com.SOVCS.DirParser.DirectoryParser;
import com.SOVCS.StateMachine.FileState;
import com.SOVCS.StateMachine.GetQuery;
import com.SOVCS.StateMachine.MapStateMachine;
import com.SOVCS.StateMachine.PutCommand;
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.catalyst.util.Listener;
import io.atomix.copycat.client.CopycatClient;
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.server.storage.StorageLevel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String [] args) throws IOException{
        String IP = "pi.cs.oswego.edu";
        System.out.println("Connecting on " + IP);
        int port = 2753;
        System.out.println("Connecting on port " + port);
        Address myAddress = new Address(IP, port);
        CopycatServer server = null;

        if(args.length != 1) {
            System.out.println("Please indicate either pi, rho or wolf.");
            System.exit(0);
        } else if(args[0].compareToIgnoreCase("pi") == 0) {
            //bootstrap on pi
            IP = "pi.cs.oswego.edu";
            myAddress = new Address(IP, port);
            ArrayList<Address> cluster = new ArrayList<>();

            server = serverBuilder(IP, port, cluster);

        } else if(args[0].compareToIgnoreCase("rho") == 0) {
            //connect to the cluster of {pi} from rho
            IP = "rho.cs.oswego.edu";
            myAddress = new Address(IP, port);
            ArrayList<Address> cluster = new ArrayList<>();
            cluster.add(new Address("pi.cs.oswego.edu", port));
            cluster.add(myAddress);

            server = serverBuilder(IP, port, cluster);

        } else if(args[0].compareToIgnoreCase("wolf") == 0) {
            //connect to the cluster of {pi, rho} from wolf
            IP = "wolf.cs.oswego.edu";
            myAddress = new Address(IP, port);
            ArrayList<Address> cluster = new ArrayList<>();
            cluster.add(new Address("pi.cs.oswego.edu", port));
            cluster.add(new Address("rho.cs.oswego.edu", port));
            cluster.add(myAddress);

            server = serverBuilder(IP, port, cluster);

        } else {
            System.out.println("Please indicate either pi, rho or wolf.");
            System.exit(0);
        }

        CopycatClient client = clientBuilder();
        client.connect(myAddress).thenRun(() -> System.out.println("Successfully connected to the cluster as a client!"));

        //Specify a directory
        String directory = "Sync";
        //Create the directory parser and read in last mod times
        DirectoryParser dirParse = new DirectoryParser();
        dirParse.updateTimes(directory);

        boolean firstRun = false;

        if(args[0].compareToIgnoreCase("pi") != 0){
            firstRun = true;
        }

        //Infinitely run the main cycle
        while(true){
            //Store the returned DirectoryParser object to remember last mod times
            System.out.println("The server is currently a " + server.state());
            if(firstRun){
                dirParse = runCycle(client, dirParse, directory, myAddress, firstRun);
                firstRun = false;
            }
            dirParse = runCycle(client, dirParse, directory, myAddress, false);
        }

    }

    private static DirectoryParser runCycle(CopycatClient client, DirectoryParser dirParse, String directory, Address myAddress, boolean init){
        //Wait for given time in seconds
        if(!init){
            wait(30);
        }
        System.out.println("Running cycle...");
        //Get arraylist of all changed files
        ArrayList<File> changedFiles = dirParse.detectChanges();
        if(changedFiles.isEmpty()){
            System.out.println("No changes detected...");
        }else{
            System.out.println("Changes detected... Pushing changes...");
            //Push changes to the state machine
            for(File file:changedFiles) {
                FileState fs = new FileState(file, myAddress);
                fs.setBytes(toBytes(fs));
                pushChanges(client, fs);
            }
        }
        //Update the object with new mod times
        dirParse.updateTimes(directory);

        //Get arraylist of all files
        ArrayList<File> allFiles = dirParse.findAllFiles(directory);
        if(!allFiles.isEmpty()){
            //Check if there is an update on the state machine for all files
            for(File file : allFiles){
                FileState fs = new FileState(file, myAddress);
                fs.setBytes(toBytes(fs));
                checkIfUpdated(client, fs);
            }
            //Update the object with new mod times
            dirParse.updateTimes(directory);
        }else{
            System.out.println("The directory is currently empty...");
        }
        return dirParse;
    }

    private static void wait(int timeInSeconds){
        //Wait for the given time in seconds
        try {
            Thread.sleep(timeInSeconds*1000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void pushChanges(CopycatClient client, FileState fs){
        //Update the state machine with the updated file
        CompletableFuture<Object> future = client.submit(new PutCommand(fs.getKey(), fs));
        Object result = future.join();

    }

    private static byte[] toBytes(FileState fs){
        byte[] bytes = new byte[0];
        java.nio.file.Path path = java.nio.file.Paths.get(fs.getFile().getPath() + "/" + fs.getFile().getName());
        try {
            bytes = java.nio.file.Files.readAllBytes(path);
        } catch(IOException ioe) {
            System.out.println(ioe.toString());
        }
        return bytes;
    }

    private static void checkIfUpdated(CopycatClient client, FileState currentfs){
        //Submit a query asking for a FileState object
        client.submit(new GetQuery(currentfs.getKey())).thenAccept(result -> {
            //Check to see if the checksums don't match if not update that file
            if(!currentfs.getChecksum().equals(result.getChecksum())){
                System.out.println("Local file does not match the one on the state machine... updating file...");
                updateFile(result);
            }
        });
    }

    private static void updateFile(FileState fs){
        String IP = fs.getAddress().host();
        int port = fs.getAddress().port();
        String filePath = fs.getFile().getPath() + "/" + fs.getFile().getName();
        File file = new File(filePath);
        file.delete();
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write(fs.getBytes());
            fos.close();
        } catch (IOException e){
            System.exit(0);
        }
        System.out.println(fs.getFile().getName() + " has been updated successfully!");
    }

    private static CopycatServer serverBuilder(String IP, int port, ArrayList<Address> hosts) throws UnknownHostException{
        Address address = new Address(IP, port);
        //Address clientAddress = new Address(IP, port+1);

        CopycatServer server = CopycatServer.builder(address)
                .withStateMachine(MapStateMachine::new)
                .withTransport(NettyTransport.builder()
                        .withThreads(4)
                        .build())
                .withStorage(Storage.builder()
                        .withDirectory(new File("logs"))
                        .withStorageLevel(StorageLevel.DISK)
                        .build())
                .build();

        server.serializer()
                .register(com.SOVCS.StateMachine.PutCommand.class)
                .register(com.SOVCS.StateMachine.GetQuery.class);

        Listener<CopycatServer.State> stateChangeListener = server.onStateChange((state) ->
                System.err.println("Server::Server - onStateChange: new state " + state.name()));

        if(hosts.isEmpty()) {
            server.bootstrap().join();
            System.out.println("Server successfully bootstrapped");
        }else{
            server.join(hosts).join();
        }

        return server;
    }

    private static CopycatClient clientBuilder(){
        CopycatClient.Builder builder = CopycatClient.builder();

        CopycatClient client = CopycatClient.builder()
                .withTransport(NettyTransport.builder()
                        .withThreads(2)
                        .build())
                .build();

        client.serializer()
                .register(com.SOVCS.StateMachine.PutCommand.class)
                .register(com.SOVCS.StateMachine.GetQuery.class);

        return client;
    }

    //method to create a checksum from a file
    //input is the file name as a string.
    //return is the string checksum

    public static String checksum(String file) throws Exception{
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