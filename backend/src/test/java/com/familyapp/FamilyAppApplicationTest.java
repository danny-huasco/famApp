package com.familyapp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FamilyAppApplicationTest {
    @Test
    void applicationPackageIsStable() {
        assertEquals("com.familyapp", FamilyAppApplication.class.getPackageName());
    }
}
