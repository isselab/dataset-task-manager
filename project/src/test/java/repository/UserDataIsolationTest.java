package repository;

import org.junit.jupiter.api.Test;
import service.TaskService;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserDataIsolationTest {
    @Test
    void taskDataIsSeparatedBetweenUserRepositories() throws Exception {
        var directory = Files.createTempDirectory("task-manager-users");
        var aliceService = new TaskService(new JsonTaskRepository(directory.resolve("alice.json")));
        aliceService.createTask("Alice's private task");
        aliceService.save();

        var bobService = new TaskService(new JsonTaskRepository(directory.resolve("bob.json")));
        bobService.load();
        assertEquals(0, bobService.getTasks().size());

        var reloadedAlice = new TaskService(new JsonTaskRepository(directory.resolve("alice.json")));
        reloadedAlice.load();
        assertEquals(1, reloadedAlice.getTasks().size());
        assertEquals("Alice's private task", reloadedAlice.getTasks().getFirst().getTitle());
    }
}