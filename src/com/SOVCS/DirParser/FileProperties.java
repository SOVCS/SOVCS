package com.SOVCS.DirParser;

import java.io.File;

//Class written by Justin

public class FileProperties {

    //The file
    private File file;
    //All properties
    private long lastModTime;

    public FileProperties(File file){
        //Save file
        this.file = file;
        //Grab all properties of file needed
        this.lastModTime = file.lastModified();
        //More properties can eventually added here if needed
    }

    //Getters and Setters
    public File getFile(){
        return this.file;
    }
    public void setFile(File file){
        this.file = file;
    }

    public long getLastModTime(){
        return this.lastModTime;
    }
    public void setLastModTime(long lastModTime){
        this.lastModTime = lastModTime;
    }
}
