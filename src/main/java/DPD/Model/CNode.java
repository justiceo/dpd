package DPD.Model;

/**
 * Created by Justice on 1/10/2017.
 */
public class CNode {
    public int classId = -1;
    public int pocket = -1;
    public int score = 0;

    public CNode(int classId, int pocket) {
        this.classId = classId;
        this.pocket = pocket;
    }

    @Override
    public String toString() {
        return String.valueOf(classId);
    }
}