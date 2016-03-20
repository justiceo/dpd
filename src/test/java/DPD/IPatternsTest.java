package DPD;

import DPD.Enums.ClassType;
import DPD.DSMMapper.IPattern;
import DPD.DSMMapper.PatternEntity;
import DPD.DSMMapper.PatternRule;
import DPD.Enums.RuleType;
import DPD.PatternParser.CommonPatternsParser;
import DPD.PatternParser.IPatternsParser;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Justice on 1/27/2016.
 */
public class IPatternsTest { // tests that loaded patterns contain the right information

    private IPattern observerPattern;
    private final String configFilePath = "D:\\Code\\IdeaProjects\\DesignPatterns\\config.xml";

    @Before
    public void setup() {
        IPatternsParser patternsParser = new CommonPatternsParser();
        patternsParser.init(new File(configFilePath));
        observerPattern = patternsParser.parse(patternsParser.getRunnableConfigs().get(0));
    }

    @After
    public void tearDown() {
        observerPattern = null;
    }

    @Test
    public void getEntitiesTest() {
        List<PatternEntity> actualEntityList = observerPattern.getEntities();
        Assert.assertEquals(3, actualEntityList.size());
        PatternEntity firstEntity = new PatternEntity();
        firstEntity.id = "e1";
        firstEntity.name = "Observer";
        firstEntity.type = ClassType.Interface;
        Assert.assertTrue(firstEntity.equals(actualEntityList.get(0)));
    }

    @Test
    public void getRulesTest() {
        List<PatternRule> actualRules = observerPattern.getRules();
        //assertEquals(7, actualRules.size());
        PatternRule firstRule = new PatternRule();
        firstRule.ruleType = RuleType.Dependency;
        firstRule.source = "e2";
        firstRule.value = "Implement";
        firstRule.target = "e1";
        PatternRule actual1 = actualRules.get(0);
        Assert.assertTrue(firstRule.equals(actualRules.get(0)));
    }
}