package DPD.REPL;

import DPD.Browser.EasyDSMQuery;
import DPD.Model.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.*;

/**
 * Created by Justice on 1/10/2017.
 */
public class Environment {

    private HashMap<String, String> declaredVariables;
    private HashMap<String, Bucket> bucketList;
    private EasyDSMQuery dsmQuery;
    private OperatorFunctions opFunc;

    public Environment(EasyDSMQuery dsmBrowser) {
        declaredVariables = new HashMap<>();
        bucketList = new HashMap<>();
        this.dsmQuery = dsmBrowser;
        opFunc = new OperatorFunctions(dsmBrowser);
    }

    public void createEntity(String entityId, String name) throws Exception {
        assertUndeclared(entityId);
        declaredVariables.put(entityId, name);
    }

    public void createBucket(String bucketId, String name) throws Exception {
        assertUndeclared(bucketId);

        declaredVariables.put(bucketId, name);
        bucketList.put(bucketId, new Bucket());
    }

    public BucketResult evalDependency(DependencyType dependency, String leftOperand, String rightOperand) throws Exception {
        return evalDependency(toList(dependency), leftOperand, rightOperand);
    }

    public BucketResult evalDependency(List<DependencyType> dependency, String leftOperand, String rightOperand) throws Exception {
        assertDeclared(leftOperand, rightOperand);
        BucketResult t = new BucketResult();
        dsmQuery.populate(dependency, t);

        t.put(leftOperand, t.aux);
        t.put(rightOperand, t.pivot);
        setGroupId(t, t.get(rightOperand));
        return t;
    }

    public BucketResult evalFunction(String bucketId, String operator, String... operands) throws Exception {
        return evalFunction(bucketList.get(bucketId), operator, operands);
    }

    public BucketResult evalFunction(Bucket bucket, String operator, String... operands) throws Exception {
        OperatorObject op = opFunc.get(operator);
        if( op == null )
            throw new NotImplementedException();

        return op.func.call(bucket, operands[0], operands[1], null);
    }

    public Bucket evalBucketStatement(String bucketId, Evaluator.StatementType action, BucketResult bResult) throws Exception {
        assertDeclared(bucketId);
        Bucket b = bucketList.get(bucketId);
        return evalBucketStatement(b, action, bResult);
    }

    public static Bucket evalBucketStatement(Bucket b, Evaluator.StatementType action, BucketResult bResult) throws Exception {
        bResult.keySet().forEach(k -> b.addIfNotExists(k));
        switch (action) {
            case FillStatement:
                BucketResult finalBResult1 = bResult;
                bResult.keySet().forEach(k -> b.get(k).addAll(finalBResult1.get(k).toList()));
                break;
            case OverwriteStatement:
                // this has a filter effect, so elements should already exist in entity
                bResult = trimToMatchBucket(b, bResult);
                for(String k: bResult.keySet()) {
                    b.get(k).clear();
                    b.get(k).addAll(bResult.get(k).toList());
                }
                break;
            case FilterStatement:
                // this has a filter effect, so elements should already exist in entity
                bResult = trimToMatchBucket(b, bResult);
                BucketResult finalBResult = bResult;
                bResult.keySet().forEach(k -> b.get(k).removeAll(finalBResult.get(k).toList()));
                break;
            case PromoteStatement:
                // necessary that we're promoting only items already in the entity
                bResult = trimToMatchBucket(b, bResult);
                finalBResult = bResult;
                bResult.keySet().forEach(k -> b.get(k).promoteAll(finalBResult.get(k).toList()));
                break;
            case DemoteStatement:
                bResult = trimToMatchBucket(b, bResult);
                finalBResult = bResult;
                bResult.keySet().forEach(k -> b.get(k).demoteAll(finalBResult.get(k).toList()));
                break;
        }
        return b;
    }

    public Bucket resolveBucket(String bucketId) throws Exception {
        assertDeclared(bucketId);

        Bucket b = bucketList.get(bucketId);
        for(int pocket = 0; pocket <= b.getPocket(); pocket++) {
            boolean allHaveIt = true;
            for(String entityId: b.keySet()) {
                allHaveIt = allHaveIt && b.get(entityId).hasPocket(pocket);
            }
            if(!allHaveIt) {
                for(String entityId: b.keySet()) {
                    b.get(entityId).removePocket(pocket);
                }
            }
        }
        return b;
    }

    public Bucket unifyEntity(Entity e) throws Exception {
        return null;
    }

    /**
     * Prints bucket and bucket entities if specified
     * input types bucket or bucket.entity
     * @param objectId
     * @throws Exception
     */
    public void printObject(String objectId) throws Exception {
        if(bucketList.containsKey(objectId))
            printBucket(objectId); //System.out.println(objectId + ":\n" +bucketList.get(objectId));
        else if(objectId.contains(".")) {
            String[] vars =  objectId.split(".");
            String entity = vars.length > 1 ? vars[1] : "";
            if( declaredVariables.containsKey(vars[0]) && declaredVariables.containsKey(entity))
                System.out.println(bucketList.get(vars[0]).get(vars[1]));
        }
    }

    public void printBucket(String bucketId) {
        if( !bucketList.containsKey(bucketId) ) return;
        System.out.println("Bucket " + bucketId);
        Bucket b = bucketList.get(bucketId);
        for(String eKey: b.keySet()){
            System.out.print("\tEntity " + eKey + ": ");
            for(CNode c: b.get(eKey).toList()) {
                System.out.print(dsmQuery.GetType(c.classId) + "(" + c.pocket + ")" + ", ");
            }
            System.out.println();
        }
        System.out.println("\n---------------------------\n");
    }

    public void printByPocket(String bucketId) {
        if( !bucketList.containsKey(bucketId) ) return;
        System.out.println("Bucket " + bucketId);

        class PocketV {
            int pocketId = -1;
            double score = 0;
            String str = "";

            public PocketV(int p, int s, String print){
                pocketId = p; score = s; str = print;
            }
        }

        List<PocketV> pockets = new ArrayList<>();

        Bucket b = bucketList.get(bucketId);
        for(int i = 0; i <= b.getPocket(); i++) {
            if( !b.isPocketInAnyEntity(i) ) continue;
            String pocketStr = "";
            int score = 0;
            for(String eKey: b.keySet()){
                pocketStr += ("\tEntity " + eKey + ": ");
                double localScore = 0;
                int acceptedClasses = 0;
                Entity entity = b.get(eKey);
                for(CNode c: entity.toList()) {
                    if(c.pocket == i) {
                        pocketStr += (dsmQuery.GetType(c.classId) + "(" + c.pocket + ")" + ", ");
                        localScore += c.score;
                        ++acceptedClasses;
                    }
                }
                //localScore = localScore / (entity.getMaxScore() * acceptedClasses);
                score += localScore;
                pocketStr += "\n";
            }
            PocketV pocketV = new PocketV(i, score, pocketStr);
            pockets.add(pocketV);
        }

        pockets.sort(Comparator.comparing(p -> p.score));
        Collections.reverse(pockets);

        for(PocketV p: pockets) {
            System.out.print(p.str);
            System.out.print("\tscore: " + p.score + "\n\n");
        }
        System.out.println("\n---------------------------\n");
    }

    private void setGroupId(BucketResult t, Entity entity) {
        // t.pivot is the pivot by default
        for(CNode cn: t.pivot.toList()) {
            if(entity.hasClass(cn.classId)) {
                int existingPocket = entity.get(cn.classId).pocket;
                for(int i: t.aux.keySet()){
                    if(t.aux.get(i).pocket == cn.pocket){
                        t.aux.get(i).pocket = existingPocket;
                    }
                }
                cn.pocket = existingPocket;
            }
        }
    }

    public void assertDeclared(String... variableIds) throws Exception {
        for(String var: variableIds) {
            if( !declaredVariables.containsKey(var) )
                throw new Exception(var + " is an undeclared variable");
        }
    }

    private void assertUndeclared(String... variableIds) throws Exception {
        for(String var: variableIds) {
            if( declaredVariables.containsKey(var) )
                throw new Exception(var + " is already defined");
        }
    }

    private boolean isDefined(Bucket b, String... variableIds) {
        return b.keySet().containsAll(Arrays.asList(variableIds));
    }

    public static <T> List<T> toList( T input ) {
        List<T> result = new ArrayList<T>();
        result.add(input);
        return result;
    }

    // removes every class or entity in composite that is not in principal
    // hence we're are trimming composite contents to match the elements in principal
	public static BucketResult trimToMatchBucket(Bucket principal, BucketResult composite) {
		// for each entity in composite bucket, if it exists, replace otherwise remove
        BucketResult result = new BucketResult();
		for(String key: composite.keySet()){
			Entity pE = principal.get(key);
            if(pE == null) { // entity doesn't exist, so we don't have to add it's classes
                continue;
            }
			Entity cE = composite.get(key);
            Entity e = new Entity();
            for(CNode cn: cE.toList()) {
                if(pE.hasClass(cn.classId)) {
                    e.add(pE.get(cn.classId).cloneTo(cn));
                }
            }
            result.put(key, e);
		}
		return result;
	}

}
