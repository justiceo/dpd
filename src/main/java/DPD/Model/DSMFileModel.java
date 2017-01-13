package DPD.Model;

import DPD.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Created by Justice on 3/20/2016.
 * Represents the model of a DSM file
 */
public class DSMFileModel extends FileModel {

    public DSMFileModel(String fileName) {
        super(fileName);
    }

    public void loadFile(String dsmFilePath) {

        Scanner in = null;
        try {
            in = new Scanner(new File(dsmFilePath));
        } catch (FileNotFoundException e) {
            Logger.getGlobal().severe("Dsm file does not exist: " + dsmFilePath);
            return;
        }

        exhibitedDependencyLine = in.nextLine();
        int matrixSize = Integer.parseInt(in.nextLine());

        matrixLines = new String[matrixSize];
        for (int i = 0; i < matrixSize; i++) {           /* read the matrix */
            matrixLines[i] = in.nextLine();
        }

        filePaths = new String[matrixSize];
        for (int i = 0; i < matrixSize; i++) {                    /* read the java files */
            filePaths[i] = Util.fixFilePath(in.nextLine());
        }

        // close file input
        in.close();
    }
}
