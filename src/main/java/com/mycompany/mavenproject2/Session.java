package com.mycompany.mavenproject2;

import com.mycompany.mavenproject2.Models.AccountRole;
import com.mycompany.mavenproject2.Models.User;

/**
 * LOGIN/logout
 *
 * @author Ariel Soriano
 */
public final class Session {
    private static User current;
    private Session() {
    }

    static User user() {
        return current;
    }

    static void start(User user) {
        current = user;
    }

    static void end() {
        current = null;
    }

    static boolean isProfessor() {
        return current != null && current.role() == AccountRole.PROFESSOR;
    }
}
