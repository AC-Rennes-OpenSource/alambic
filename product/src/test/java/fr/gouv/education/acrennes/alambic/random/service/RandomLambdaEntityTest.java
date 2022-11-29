package fr.gouv.education.acrennes.alambic.random.service;

import fr.gouv.education.acrennes.alambic.random.persistence.RandomLambdaEntity;
import org.junit.Assert;
import org.junit.Test;

public class RandomLambdaEntityTest {
    @Test
    public void test1() {
        RandomLambdaEntity rle = new RandomLambdaEntity("{\"value\":\"848657\"}");
        Assert.assertEquals("i9TNZ9PsSJA3QK1GoPRQCQ==", rle.getHash());
    }
}
