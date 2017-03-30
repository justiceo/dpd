package DPD.Model;

import java.io.Serializable;

/**
 * Created by Justice on 1/10/2017.
 */
public class CNode implements Serializable{
    public int classId = -1;
    public int pocket = -1;
    public int score = 0;

    public CNode(int classId, int pocket) {
        this.classId = classId;
        this.pocket = pocket;
    }

    @Override
    public String toString() {
        return "{classId: " + classId + ", pocket: " + pocket + "}";
    }

    @Override
    public boolean equals(Object other) {
        CNode c = (CNode) other;
        return classId == c.classId && pocket == c.pocket && score == c.score;
    }

    public CNode cloneTo(CNode cn) {
        cn.pocket = pocket;
        cn.classId = classId;
        cn.score = score;
        return cn;
    }
}