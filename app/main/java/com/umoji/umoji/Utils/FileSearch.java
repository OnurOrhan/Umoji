package com.umoji.umoji.Utils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by User on 7/24/2017.
 */

public class FileSearch {
    /**
     * Search a directory and return a list of all **directories** contained inside
     * @param directory
     * @return
     */
    public static ArrayList<String> getDirectoryPaths(String directory){
        ArrayList<String> pathArray = new ArrayList<>();

        File file = new File(directory);
        File[] listfiles = file.listFiles();

        if(listfiles != null) {
            for(File f : listfiles) {
                if(f.isDirectory() && !f.isHidden()){
                    pathArray.add(f.getAbsolutePath());
                }
            }
        } return pathArray;
    }

    public static void addDirectoryPaths( ArrayList<String> pathArray, String directory){
        File file = new File(directory);
        File[] list = file.listFiles();

        if(list != null) {
            for(File f : list) {
                if(f.isDirectory() && !f.isHidden()){
                    pathArray.add(f.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Search a directory and return a list of all **files** contained inside
     * @param directory
     * @return
     */
    public static ArrayList<String> getFilePaths(String directory){
        ArrayList<String> pathArray = new ArrayList<>();

        File file = new File(directory);
        File[] list = file.listFiles();

        if(list != null) {
            for(File f : list) {
                if(f.isFile()){
                    pathArray.add(f.getAbsolutePath());
                }
            }
        } return pathArray;
    }
}
