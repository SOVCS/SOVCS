package com.SOVCS.DirParser;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

//Class written by Justin

public class DirectoryParser {

    private ArrayList<FileProperties> updatedTimes;

    public void updateTimes(String directory){
        ArrayList<FileProperties> updatedTimes = new ArrayList<>();
        //Get all files in given directory and sub directories
        ArrayList<File> allFileDirs = findAllFiles(directory);
        for(File file:allFileDirs){
            /*
            Parse through all those files create a FileProperties object
            Append the FileProperties object to another arraylist
            File properties will be grabbed within class(like lastmodtime)
            */
            updatedTimes.add(new FileProperties(file));
        }
        //Save to arraylist outside this method
        this.updatedTimes = updatedTimes;
    }

    public ArrayList<File> detectChanges(){
        ArrayList<File> modifiedFiles = new ArrayList<>();
        /*
        Search all FileProperties objects in arraylist
        Compare stored mod times to live mod times
        If they do not match then the file was changed
        So add file object to arraylist
         */
        for(FileProperties fp:this.updatedTimes){
            if(fp.getLastModTime() != fp.getFile().lastModified()){
                modifiedFiles.add(fp.getFile());
            }
        }
        //Return arraylist of all modified files
        return modifiedFiles;
    }

    public ArrayList<File> findAllFiles(String directory){
        final ArrayList<File> dirs = new ArrayList<File>();
        //Create file object with given directory
        File f = new File(directory);
        File[] allFiles = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                //Store the location of the found item
                String location = dir.getPath() + "/" + name;
                //Create a file object with that location
                File path = new File(location);
                if (path.isDirectory()){
                    //If it is a directory(not a file) recursively call this method
                    //and add the returned arraylist to the local one
                    dirs.addAll(findAllFiles(location));
                    return false;
                }else {
                    //If not a directory add to the local directory list
                    dirs.add(path);
                    return true;
                }
            }
        });
        //Return the directories of all the files found
        return dirs;
    }

}
