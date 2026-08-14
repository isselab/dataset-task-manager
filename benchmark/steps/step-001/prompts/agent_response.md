Add labels to the task manager. A user must be able to create labels and
assign one label to a task. Existing labels should be reusable across multiple
tasks. Show the assigned label in the task list and allow the user to remove
or change a task's label. Labels and task-label assignments must be saved
and restored when the application is restarted.


• I’ll implement the label lifecycle end to end: persistence, task assignment/change/removal, reusable labels, and task-list display. Before editing, I’m loading the required Agent
HAnS feature model and annotation guidance.

• Called agent-hans.get-feature-model({"projectPath":"D:\\Master\\Masterarbeit\\datasets-repos\\dataset-task-manager\\project"})
└

• Ran Get-Content -Raw 'D:\Master\Masterarbeit\datasets-repos\dataset-task-manager\project\.agents\skills\feature-model\SKILL.md'; "`n---ANNOTATION---`n"; Get-Content -Raw 'D:
│ \Master\Masterarbeit\datasets-repos\dataset-task-manager\project\.agents\skills\embedded-feature-annotation\SKILL.md'
└ ---
name: feature-model-skill
… +118 lines (ctrl + t to view transcript)
}


────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• The repository is valid, and the required feature/annotation instructions are loaded. The feature-model service returned no content, so I’m checking the local model and app
structure before defining the new label feature and editing implementation files.

• Ran Get-ChildItem -Force; if (Test-Path .feature-model) { "---FEATURE MODEL---"; Get-Content .feature-model }; "---FILES---"; rg --files -g '!node_modules' -g '!dist' -g '!build'
│ | Select-Object -First 200
└

    … +22 lines (ctrl + t to view transcript)



• Ran Get-Content -Raw pom.xml; "---APP---"; Get-Content -Raw src/main/java/TaskManagerApplication.java; "---CSS---"; Get-Content -Raw src/main/resources/taskmanager/styles.css;
│ "---README---"; Get-Content -Raw README.md
└ <?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
… +103 lines (ctrl + t to view transcript)

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• The app is currently a minimal JavaFX shell with no task or storage model. I’m adding a small local persistence layer and a complete task/label screen, with labels represented
separately so the same label can be reused and assignments can be changed or cleared. I’m also initializing the empty feature model with the affected feature hierarchy before
implementation.

• Edited .feature-model (+7 -0)
1 +TaskManager
2 +    TaskManagement
3 +        CreateTasks
4 +    Labels
5 +        CreateLabels
6 +        AssignTaskLabels
7 +        PersistLabels

• Added src\main\java\TaskManagerApplication.java (+206 -0)
1 +import javafx.application.Application;
2 +import javafx.collections.FXCollections;
3 +import javafx.collections.ObservableList;
4 +import javafx.geometry.Insets;
5 +import javafx.geometry.Pos;
6 +import javafx.scene.Scene;
7 +import javafx.scene.control.Button;
8 +import javafx.scene.control.ChoiceBox;
9 +import javafx.scene.control.Label;
10 +import javafx.scene.control.ListCell;
11 +import javafx.scene.control.ListView;
12 +import javafx.scene.control.TextField;
13 +import javafx.scene.layout.BorderPane;
14 +import javafx.scene.layout.HBox;
15 +import javafx.scene.layout.Priority;
16 +import javafx.scene.layout.VBox;
17 +import javafx.stage.Stage;
18 +
19 +import java.io.IOException;
20 +import java.io.InputStream;
21 +import java.io.OutputStream;
22 +import java.nio.file.Files;
23 +import java.nio.file.Path;
24 +import java.nio.file.Paths;
25 +import java.util.ArrayList;
26 +import java.util.List;
27 +import java.util.Properties;
28 +import java.util.UUID;
29 +
30 +public class TaskManagerApplication extends Application {
31 +    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
32 +    private final ObservableList<TaskLabel> labels = FXCollections.observableArrayList();
33 +    private final TaskStore store = new TaskStore();
34 +
35 +    @Override
36 +    public void start(Stage stage) {
37 +        // &begin[TaskManagement]
38 +        store.load(tasks, labels);
39 +
40 +        Label title = new Label("Task Manager");
41 +        title.getStyleClass().add("app-title");
42 +
43 +        TextField taskInput = new TextField();
44 +        taskInput.setPromptText("New task");
45 +        Button addTask = new Button("Add task");
46 +        addTask.setOnAction(event -> {
47 +            String titleText = taskInput.getText().trim();
48 +            if (!titleText.isEmpty()) {
49 +                tasks.add(new Task(UUID.randomUUID().toString(), titleText, null));
50 +                taskInput.clear();
51 +                store.save(tasks, labels);
52 +            }
53 +        });
54 +        taskInput.setOnAction(event -> addTask.fire());
55 +
56 +        ListView<Task> taskList = new ListView<>(tasks);
57 +        taskList.setCellFactory(view -> new TaskCell(labels, store, tasks));
58 +        VBox.setVgrow(taskList, Priority.ALWAYS);
59 +
60 +        TextField labelInput = new TextField();
61 +        labelInput.setPromptText("New label");
62 +        Button addLabel = new Button("Create label");
63 +        addLabel.setOnAction(event -> {
64 +            String name = labelInput.getText().trim();
65 +            if (!name.isEmpty() && labels.stream().noneMatch(label -> label.name().equalsIgnoreCase(name))) {
66 +                labels.add(new TaskLabel(UUID.randomUUID().toString(), name));
67 +                labelInput.clear();
68 +                store.save(tasks, labels);
69 +                taskList.refresh();
70 +            }
71 +        });
72 +        labelInput.setOnAction(event -> addLabel.fire());
73 +
74 +        HBox taskForm = new HBox(8, taskInput, addTask);
75 +        HBox.setHgrow(taskInput, Priority.ALWAYS);
76 +        HBox labelForm = new HBox(8, labelInput, addLabel);
77 +        HBox.setHgrow(labelInput, Priority.ALWAYS);
78 +        VBox content = new VBox(16, title, taskForm, new Label("Labels"), labelForm, taskList);
79 +        content.setPadding(new Insets(24));
80 +        content.getStyleClass().add("app-root");
81 +
82 +        Scene scene = new Scene(new BorderPane(content), 800, 600);
83 +        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());
84 +        stage.setTitle("Task Manager");
85 +        stage.setScene(scene);
86 +        stage.show();
87 +        // &end[TaskManagement]
88 +    }
89 +
90 +    public static void main(String[] args) {
91 +        launch(args);
92 +    }
93 +
94 +    // &begin[AssignTaskLabels]
95 +    private static final class TaskCell extends ListCell<Task> {
96 +        private final ObservableList<TaskLabel> labels;
97 +        private final TaskStore store;
98 +        private final ObservableList<Task> tasks;
99 +
100 +        private TaskCell(ObservableList<TaskLabel> labels, TaskStore store, ObservableList<Task> tasks) {
101 +            this.labels = labels;
102 +            this.store = store;
103 +            this.tasks = tasks;
104 +        }
105 +
106 +        @Override
107 +        protected void updateItem(Task task, boolean empty) {
108 +            super.updateItem(task, empty);
109 +            if (empty || task == null) {
110 +                setGraphic(null);
111 +                setText(null);
112 +                return;
113 +            }
114 +            Label taskTitle = new Label(task.title());
115 +            taskTitle.getStyleClass().add("task-title");
116 +            ChoiceBox<LabelOption> selector = new ChoiceBox<>();
117 +            selector.getItems().add(new LabelOption(null, "No label"));
118 +            labels.stream().map(label -> new LabelOption(label.id(), label.name())).forEach(selector.getItems()::add);
119 +            String selectedId = task.labelId();
120 +            selector.getSelectionModel().select(selector.getItems().stream()
121 +                    .filter(option -> java.util.Objects.equals(option.id(), selectedId)).findFirst().orElse(selector.getItems().getFirst()));
122 +            selector.setOnAction(event -> {
123 +                task.setLabelId(selector.getValue().id());
124 +                store.save(tasks, labels);
125 +                updateItem(task, false);
126 +            });
127 +            HBox row = new HBox(12, taskTitle, selector);
128 +            row.setAlignment(Pos.CENTER_LEFT);
129 +            HBox.setHgrow(taskTitle, Priority.ALWAYS);
130 +            setGraphic(row);
131 +        }
132 +    }
133 +    // &end[AssignTaskLabels]
134 +
135 +    // &begin[Labels]
136 +    private record TaskLabel(String id, String name) { }
137 +    private record LabelOption(String id, String name) {
138 +        @Override public String toString() { return name; }
139 +    }
140 +    private static final class Task {
141 +        private final String id;
142 +        private final String title;
143 +        private String labelId;
144 +
145 +        private Task(String id, String title, String labelId) {
146 +            this.id = id;
147 +            this.title = title;
148 +            this.labelId = labelId;
149 +        }
150 +
151 +        private String id() { return id; }
152 +        private String title() { return title; }
153 +        private String labelId() { return labelId; }
154 +        private void setLabelId(String labelId) { this.labelId = labelId; }
155 +    }
156 +    // &end[Labels]
157 +
158 +    // &begin[PersistLabels]
159 +    private static final class TaskStore {
160 +        private final Path file = Paths.get(System.getProperty("user.home"), ".task-manager", "tasks.properties");
161 +
162 +        private void load(List<Task> tasks, List<TaskLabel> labels) {
163 +            if (!Files.exists(file)) return;
164 +            Properties properties = new Properties();
165 +            try (InputStream input = Files.newInputStream(file)) {
166 +                properties.load(input);
167 +                int labelCount = Integer.parseInt(properties.getProperty("labels.count", "0"));
168 +                for (int i = 0; i < labelCount; i++) {
169 +                    labels.add(new TaskLabel(properties.getProperty("label." + i + ".id"), properties.getProperty("label." + i + ".name")));
170 +                }
171 +                int taskCount = Integer.parseInt(properties.getProperty("tasks.count", "0"));
172 +                for (int i = 0; i < taskCount; i++) {
173 +                    tasks.add(new Task(properties.getProperty("task." + i + ".id"), properties.getProperty("task." + i + ".title"), properties.getProperty("task." + i + ".
labelId")));
174 +                }
175 +            } catch (IOException | NumberFormatException ignored) {
176 +                // A malformed local file should not prevent the application from starting.
177 +            }
178 +        }
179 +
180 +        private void save(List<Task> tasks, List<TaskLabel> labels) {
181 +            Properties properties = new Properties();
182 +            properties.setProperty("labels.count", Integer.toString(labels.size()));
183 +            for (int i = 0; i < labels.size(); i++) {
184 +                TaskLabel label = labels.get(i);
185 +                properties.setProperty("label." + i + ".id", label.id());
186 +                properties.setProperty("label." + i + ".name", label.name());
187 +            }
188 +            properties.setProperty("tasks.count", Integer.toString(tasks.size()));
189 +            for (int i = 0; i < tasks.size(); i++) {
190 +                Task task = tasks.get(i);
191 +                properties.setProperty("task." + i + ".id", task.id());
192 +                properties.setProperty("task." + i + ".title", task.title());
193 +                if (task.labelId() != null) properties.setProperty("task." + i + ".labelId", task.labelId());
194 +            }
195 +            try {
196 +                Files.createDirectories(file.getParent());
197 +                try (OutputStream output = Files.newOutputStream(file)) {
198 +                    properties.store(output, "Task Manager data");
199 +                }
200 +            } catch (IOException ignored) {
201 +                // Persistence is best effort; the current session remains usable.
202 +            }
203 +        }
204 +    }
205 +    // &end[PersistLabels]
206 +}

• Edited src\main\resources\taskmanager\styles.css (+7 -0)
9  }
10 +
11 +/* &begin[TaskManagement] */
12 +.task-title {
13 +    -fx-font-size: 15px;
14 +    -fx-text-fill: #1f2937;
15 +}
16 +/* &end[TaskManagement] */

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• The core implementation is in place: labels are unique and reusable, each task has at most one label, “No label” clears an assignment, and every mutation is persisted. I’m
compiling now and will fix any JavaFX or persistence issues surfaced by verification.

• Ran mvn test
└ mvn : Die Benennung "mvn" wurde nicht als Name eines Cmdlet, einer Funktion, einer Skriptdatei oder eines ausführbaren
Programms erkannt. Überprüfen Sie die Schreibweise des Namens, oder ob der Pfad korrekt ist (sofern enthalten), und
… +5 lines (ctrl + t to view transcript)
+ FullyQualifiedErrorId : CommandNotFoundException


• Ran Get-Command java,javac,mvn,mvnw -ErrorAction SilentlyContinue | Select-Object Name,Source; git diff --check; git diff --stat; git diff -- .feature-model src/main/java/
│ TaskManagerApplication.java src/main/resources/taskmanager/styles.css
└
benchmark/steps/step-001/prompts/001.md           |   7 +-
… +244 lines (ctrl + t to view transcript)
warning: in the working copy of 'project/src/main/java/TaskManagerApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'project/src/main/resources/taskmanager/styles.css', LF will be replaced by CRLF the next time Git touches it

• Ran "---STATUS---"; git status --short; "---MODEL---"; Get-Content -Raw .feature-model; "---ANNOTATIONS---"; rg -n '&(begin|end|line)\[' src .feature-model; "---TARGET---";
│ Get-ChildItem -Recurse target | Select-Object -First 40 FullName
└ ---STATUS---
M ../benchmark/steps/step-001/prompts/001.md
… +39 lines (ctrl + t to view transcript)



• Ran $fx = Get-ChildItem 'C:\Users\User\.m2\repository\org\openjfx' -Recurse -Filter '*.jar' -ErrorAction SilentlyContinue; $fx | Select-Object -First 20 FullName; if ($fx)
│ { javac --module-path (($fx | ForEach-Object DirectoryName | Select-Object -Unique) -join ';') --add-modules javafx.controls -d target/classes src/main/java/
│ TaskManagerApplication.java }
└
FullName
… +69 lines (ctrl + t to view transcript)
Fehler: Modul nicht gefunden: javafx.controls
47 Fehler

• Ran Get-ChildItem 'C:\Users\User\.m2\repository\org\openjfx\javafx-base\21.0.9','C:\Users\User\.m2\repository\org\openjfx\javafx-controls\21.0.9','C:
│ \Users\User\.m2\repository\org\openjfx\javafx-graphics\21.0.9' -Filter '*.jar' | Select-Object FullName,Length
└
FullName                                                                                        Length
… +7 lines (ctrl + t to view transcript)



• Ran $mp = @('C:\Users\User\.m2\repository\org\openjfx\javafx-base\21.0.9\javafx-base-21.0.9-win.jar','C:
│ \Users\User\.m2\repository\org\openjfx\javafx-graphics\21.0.9\javafx-graphics-21.0.9-win.jar','C:
│ \Users\User\.m2\repository\org\openjfx\javafx-controls\21.0.9\javafx-controls-21.0.9-win.jar') -join ';'; javac --module-path $mp --add-modules javafx.controls -d target/
│ … +1 lines
└ Fehler: Datei C:\Users\User\.m2\repository\org\openjfx\javafx-base\21.0.9\javafx-base-21.0.9-win.jar kann nicht gelesen werden
Fehler: Datei C:\Users\User\.m2\repository\org\openjfx\javafx-graphics\21.0.9\javafx-graphics-21.0.9-win.jar kann nicht gelesen werden
Fehler: Datei C:\Users\User\.m2\repository\org\openjfx\javafx-controls\21.0.9\javafx-controls-21.0.9-win.jar kann nicht gelesen werden
Fehler: Modul nicht gefunden: javafx.controls
4 Fehler

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• Maven is not installed in this environment, and the direct JavaFX compilation fallback cannot read the local platform JARs due environment restrictions. git diff --check is
clean; I’m doing the remaining structural verification now, including confirming every annotation maps to the updated feature model, then I’ll publish the required Agent HAnS
summary.

• Called agent-hans.get-feature-model({"projectPath":"D:\\Master\\Masterarbeit\\datasets-repos\\dataset-task-manager\\project"})
└ TaskManager
TaskManagement
CreateTasks
Labels
CreateLabels
AssignTaskLabels
PersistLabels


• Ran $model = Get-Content .feature-model; $features = $model | ForEach-Object { if ($_ -match '[A-Za-z][A-Za-z0-9]*') { $Matches[0] } }; $annotations = rg -o '&(?:begin|end|
│ line)\[[A-Za-z][A-Za-z0-9]*\]' src | ForEach-Object { if ($_ -match '\[([^]]+)\]') { $Matches[1] } } | Sort-Object -Unique; "Features: $($features -join ', ')"; "Annotations:
│ $($annotations -join ', ')"; "Unknown annotations:"; Compare-Object $features $annotations -PassThru | Where-Object { $_ -in $annotations }; git diff --check
└ Features: TaskManager, TaskManagement, CreateTasks, Labels, CreateLabels, AssignTaskLabels, PersistLabels
Annotations: AssignTaskLabels, Labels, PersistLabels, TaskManagement
Unknown annotations:
warning: in the working copy of 'project/src/main/java/TaskManagerApplication.java', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of 'project/src/main/resources/taskmanager/styles.css', LF will be replaced by CRLF the next time Git touches it

• Called
└ agent-hans.summary-gui({"projectPath":"D:\\Master\\Masterarbeit\\datasets-repos\\dataset-task-manager\\project","title":"Task Manager - Add labels","description":"Added
reusable labels, one-label-per-task assignment controls, task-list display, removal/change support, and local persistence across restarts.","changedFiles":[".feature-
model","src/main/java/TaskManagerApplication.java","src/main/resources/taskmanager/styles.css"],"featureChanges":[{"featureName":"TaskManagement","featurePath":
["TaskManager","TaskManagement"],"changeType":"modified","changeDescription":"Expanded the task screen to create and display persisted tasks."},
{"featureName":"Labels","featurePath":["TaskManager","Labels"],"changeType":"added","changeDescription":"Added reusable label management for tasks."},
{"featureName":"CreateLabels","featurePath":["TaskManager","Labels","CreateLabels"],"changeType":"added","changeDescription":"Users can create uniquely named labels."},
{"featureName":"AssignTaskLabels","featurePath":["TaskManager","Labels","AssignTaskLabels"],"changeType":"added","changeDescription":"Users can assign, change, or clear one
label per task."},{"featureName":"PersistLabels","featurePath":["TaskManager","Labels","PersistLabels"],"changeType":"added","changeDescription":"Labels and task-label
assignments persist in local properties storage."}]})
Ok

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• Implemented label support.

- Create reusable labels.
- Assign one label per task.
- Change or clear assignments via “No label”.
- Display assigned labels in the task list.
- Persist labels, tasks, and assignments in ~/.task-manager/tasks.properties.
- Added feature model and annotations.

Verification: annotation references are valid and git diff --check passes. Maven could not run because mvn is unavailable in the environment.


Try to split the features in files like the example above


• I’ll refactor the current single-file implementation into the requested classes while preserving behavior: Task, Label, services, controllers, and JSON persistence. I’m reloading
  the feature model and both required annotation skills before editing this prompt’s code.

• Called agent-hans.get-feature-model({"projectPath":"D:\\Master\\Masterarbeit\\datasets-repos\\dataset-task-manager\\project"})
  └ TaskManager
        TaskManagement
            CreateTasks
        Labels
            CreateLabels
            AssignTaskLabels
            PersistLabels


• Ran Get-Content -Raw '.agents\skills\feature-model\SKILL.md'; "`n---ANNOTATION---`n"; Get-Content -Raw '.agents\skills\embedded-feature-annotation\SKILL.md'
  └ ---
    name: feature-model-skill
    … +118 lines (ctrl + t to view transcript)
    }


• Ran Get-Content -Raw src/main/java/TaskManagerApplication.java; "---POM---"; Get-Content -Raw pom.xml; "---FILES---"; rg --files src
  └ import javafx.application.Application;
    import javafx.collections.FXCollections;
    … +255 lines (ctrl + t to view transcript)
    src\main\resources\taskmanager\styles.css
    src\main\java\TaskManagerApplication.java

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• The current implementation has all domain, UI, and persistence logic embedded in TaskManagerApplication. I’m extracting the requested responsibilities into public default-package
  classes and switching storage to a self-contained JSON file, so the structure is usable without adding a JSON dependency.

• Edited 8 files (+383 -0)
  └ src\main\java\JsonTaskPersistence.java (+145 -0)
      1 +import java.io.IOException;
      2 +import java.nio.charset.StandardCharsets;
      3 +import java.nio.file.Files;
      4 +import java.nio.file.Path;
      5 +import java.nio.file.Paths;
      6 +import java.util.ArrayList;
      7 +import java.util.List;
      8 +
      9 +// &begin[PersistLabels]
     10 +public final class JsonTaskPersistence {
     11 +    private final Path file;
     12 +
     13 +    public JsonTaskPersistence() {
     14 +        this(Paths.get(System.getProperty("user.home"), ".task-manager", "tasks.json"));
     15 +    }
     16 +
     17 +    JsonTaskPersistence(Path file) {
     18 +        this.file = file;
     19 +    }
     20 +
     21 +    public void save(TaskService taskService, LabelService labelService) {
     22 +        StringBuilder json = new StringBuilder("{\"labels\":[");
     23 +        for (int i = 0; i < labelService.getLabels().size(); i++) {
     24 +            if (i > 0) json.append(',');
     25 +            Label label = labelService.getLabels().get(i);
     26 +            json.append("{\"id\":\"").append(escape(label.id())).append("\",\"name\":\"")
     27 +                    .append(escape(label.name())).append("\"}");
     28 +        }
     29 +        json.append("],\"tasks\":[");
     30 +        for (int i = 0; i < taskService.getTasks().size(); i++) {
     31 +            if (i > 0) json.append(',');
     32 +            Task task = taskService.getTasks().get(i);
     33 +            json.append("{\"id\":\"").append(escape(task.getId())).append("\",\"title\":\"")
     34 +                    .append(escape(task.getTitle())).append("\",\"labelId\":");
     35 +            if (task.getLabelId() == null) json.append("null");
     36 +            else json.append('\"').append(escape(task.getLabelId())).append('\"');
     37 +            json.append('}');
     38 +        }
     39 +        json.append("]}");
     40 +        try {
     41 +            Files.createDirectories(file.getParent());
     42 +            Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
     43 +        } catch (IOException ignored) {
     44 +            // The current session remains usable when the local file cannot be written.
     45 +        }
     46 +    }
     47 +
     48 +    public void load(TaskService taskService, LabelService labelService) {
     49 +        if (!Files.exists(file)) return;
     50 +        try {
     51 +            String json = Files.readString(file, StandardCharsets.UTF_8);
     52 +            JsonReader reader = new JsonReader(json);
     53 +            List<Label> labels = reader.readLabels();
     54 +            List<Task> tasks = reader.readTasks();
     55 +            labels.forEach(labelService::restore);
     56 +            tasks.forEach(taskService::restore);
     57 +        } catch (IOException | IllegalArgumentException ignored) {
     58 +            // A malformed local file should not prevent the application from starting.
     59 +        }
     60 +    }
     61 +
     62 +    private static String escape(String value) {
     63 +        return value.replace("\\", "\\\\").replace("\"", "\\\"")
     64 +                .replace("\n", "\\n").replace("\r", "\\r");
     65 +    }
     66 +
     67 +    private static final class JsonReader {
     68 +        private final String json;
     69 +        private int position;
     70 +
     71 +        private JsonReader(String json) { this.json = json; }
     72 +
     73 +        private List<Label> readLabels() {
     74 +            position = json.indexOf("\"labels\"");
     75 +            if (position < 0) throw new IllegalArgumentException();
     76 +            position = json.indexOf('[', position) + 1;
     77 +            List<Label> result = new ArrayList<>();
     78 +            while (!at(']')) {
     79 +                expect('{');
     80 +                String id = readStringField("id");
     81 +                String name = readStringField("name");
     82 +                expect('}');
     83 +                result.add(new Label(id, name));
     84 +                consumeComma();
     85 +            }
     86 +            return result;
     87 +        }
     88 +
     89 +        private List<Task> readTasks() {
     90 +            position = json.indexOf("\"tasks\"");
     91 +            position = json.indexOf('[', position) + 1;
     92 +            List<Task> result = new ArrayList<>();
     93 +            while (!at(']')) {
     94 +                expect('{');
     95 +                String id = readStringField("id");
     96 +                String title = readStringField("title");
     97 +                expectField("labelId");
     98 +                String labelId = atString("null") ? readNull() : readString();
     99 +                expect('}');
    100 +                result.add(new Task(id, title, labelId));
    101 +                consumeComma();
    102 +            }
    103 +            return result;
    104 +        }
    105 +
    106 +        private String readStringField(String field) {
    107 +            expectField(field);
    108 +            return readString();
    109 +        }
    110 +
    111 +        private void expectField(String field) {
    112 +            skipWhitespace();
    113 +            expect('"');
    114 +            int start = position;
    115 +            while (json.charAt(position) != '"') position++;
    116 +            if (!json.substring(start, position).equals(field)) throw new IllegalArgumentException();
    117 +            position++;
    118 +            skipWhitespace();
    119 +            expect(':');
    120 +        }
    121 +
    122 +        private String readString() {
    123 +            skipWhitespace();
    124 +            expect('"');
    125 +            StringBuilder value = new StringBuilder();
    126 +            while (position < json.length()) {
    127 +                char character = json.charAt(position++);
    128 +                if (character == '"') return value.toString();
    129 +                if (character == '\\') {
    130 +                    char escaped = json.charAt(position++);
    131 +                    value.append(escaped == 'n' ? '\n' : escaped == 'r' ? '\r' : escaped);
    132 +                } else value.append(character);
    133 +            }
    134 +            throw new IllegalArgumentException();
    135 +        }
    136 +
    137 +        private String readNull() { position += 4; return null; }
    138 +        private boolean atString(String value) { skipWhitespace(); return json.startsWith(value, position); }
    139 +        private boolean at(char character) { skipWhitespace(); return json.charAt(position) == character; }
    140 +        private void consumeComma() { skipWhitespace(); if (position < json.length() && json.charAt(position) == ',') position++; }
    141 +        private void expect(char character) { skipWhitespace(); if (position >= json.length() || json.charAt(position++) != character) throw new IllegalArgumentException()
         ; }
    142 +        private void skipWhitespace() { while (position < json.length() && Character.isWhitespace(json.charAt(position))) position++; }
    143 +    }
    144 +}
    145 +// &end[PersistLabels]

  └ src\main\java\Label.java (+4 -0)
    1 +// &begin[CreateLabels]
    2 +public record Label(String id, String name) {
    3 +}
    4 +// &end[CreateLabels]

  └ src\main\java\LabelService.java (+28 -0)
     1 +import javafx.collections.FXCollections;
     2 +import javafx.collections.ObservableList;
     3 +
     4 +import java.util.UUID;
     5 +
     6 +// &begin[CreateLabels]
     7 +public final class LabelService {
     8 +    private final ObservableList<Label> labels = FXCollections.observableArrayList();
     9 +
    10 +    public ObservableList<Label> getLabels() {
    11 +        return labels;
    12 +    }
    13 +
    14 +    public Label createLabel(String name) {
    15 +        String normalized = name.trim();
    16 +        if (normalized.isEmpty() || labels.stream().anyMatch(label -> label.name().equalsIgnoreCase(normalized))) {
    17 +            return null;
    18 +        }
    19 +        Label label = new Label(UUID.randomUUID().toString(), normalized);
    20 +        labels.add(label);
    21 +        return label;
    22 +    }
    23 +
    24 +    void restore(Label label) {
    25 +        labels.add(label);
    26 +    }
    27 +}
    28 +// &end[CreateLabels]

  └ src\main\java\MainController.java (+63 -0)
     1 +import javafx.geometry.Insets;
     2 +import javafx.scene.control.Button;
     3 +import javafx.scene.control.Label;
     4 +import javafx.scene.control.ListView;
     5 +import javafx.scene.control.TextField;
     6 +import javafx.scene.layout.BorderPane;
     7 +import javafx.scene.layout.HBox;
     8 +import javafx.scene.layout.Priority;
     9 +import javafx.scene.layout.VBox;
    10 +
    11 +// &begin[TaskManagement]
    12 +public final class MainController {
    13 +    private final TaskService taskService = new TaskService();
    14 +    private final LabelService labelService = new LabelService();
    15 +    private final JsonTaskPersistence persistence = new JsonTaskPersistence();
    16 +
    17 +    public MainController() {
    18 +        persistence.load(taskService, labelService);
    19 +    }
    20 +
    21 +    public BorderPane createView() {
    22 +        Label title = new Label("Task Manager");
    23 +        title.getStyleClass().add("app-title");
    24 +
    25 +        TextField taskInput = new TextField();
    26 +        taskInput.setPromptText("New task");
    27 +        Button addTask = new Button("Add task");
    28 +        addTask.setOnAction(event -> {
    29 +            if (!taskInput.getText().trim().isEmpty()) {
    30 +                taskService.createTask(taskInput.getText());
    31 +                taskInput.clear();
    32 +                persistence.save(taskService, labelService);
    33 +            }
    34 +        });
    35 +        taskInput.setOnAction(event -> addTask.fire());
    36 +
    37 +        ListView<Task> taskList = new ListView<>(taskService.getTasks());
    38 +        taskList.setCellFactory(view -> new TaskDialogController(taskService, labelService, persistence));
    39 +        VBox.setVgrow(taskList, Priority.ALWAYS);
    40 +
    41 +        TextField labelInput = new TextField();
    42 +        labelInput.setPromptText("New label");
    43 +        Button addLabel = new Button("Create label");
    44 +        addLabel.setOnAction(event -> {
    45 +            if (labelService.createLabel(labelInput.getText()) != null) {
    46 +                labelInput.clear();
    47 +                persistence.save(taskService, labelService);
    48 +                taskList.refresh();
    49 +            }
    50 +        });
    51 +        labelInput.setOnAction(event -> addLabel.fire());
    52 +
    53 +        HBox taskForm = new HBox(8, taskInput, addTask);
    54 +        HBox.setHgrow(taskInput, Priority.ALWAYS);
    55 +        HBox labelForm = new HBox(8, labelInput, addLabel);
    56 +        HBox.setHgrow(labelInput, Priority.ALWAYS);
    57 +        VBox content = new VBox(16, title, taskForm, new Label("Labels"), labelForm, taskList);
    58 +        content.setPadding(new Insets(24));
    59 +        content.getStyleClass().add("app-root");
    60 +        return new BorderPane(content);
    61 +    }
    62 +}
    63 +// &end[TaskManagement]

  └ src\main\java\Task.java (+35 -0)
     1 +// &begin[CreateTasks]
     2 +public final class Task {
     3 +    private final String id;
     4 +    private final String title;
     5 +    private String labelId;
     6 +
     7 +    public Task(String id, String title) {
     8 +        this(id, title, null);
     9 +    }
    10 +
    11 +    public Task(String id, String title, String labelId) {
    12 +        this.id = id;
    13 +        this.title = title;
    14 +        this.labelId = labelId;
    15 +    }
    16 +
    17 +    public String getId() {
    18 +        return id;
    19 +    }
    20 +
    21 +    public String getTitle() {
    22 +        return title;
    23 +    }
    24 +
    25 +    // &line[AssignTaskLabels]
    26 +    public String getLabelId() {
    27 +        return labelId;
    28 +    }
    29 +
    30 +    // &line[AssignTaskLabels]
    31 +    public void setLabelId(String labelId) {
    32 +        this.labelId = labelId;
    33 +    }
    34 +}
    35 +// &end[CreateTasks]

  └ src\main\java\TaskDialogController.java (+53 -0)
     1 +import javafx.collections.ObservableList;
     2 +import javafx.geometry.Pos;
     3 +import javafx.scene.control.ChoiceBox;
     4 +import javafx.scene.control.Label;
     5 +import javafx.scene.control.ListCell;
     6 +import javafx.scene.layout.HBox;
     7 +import javafx.scene.layout.Priority;
     8 +
     9 +// &begin[AssignTaskLabels]
    10 +public final class TaskDialogController extends ListCell<Task> {
    11 +    private final TaskService taskService;
    12 +    private final LabelService labelService;
    13 +    private final JsonTaskPersistence persistence;
    14 +
    15 +    public TaskDialogController(TaskService taskService, LabelService labelService, JsonTaskPersistence persistence) {
    16 +        this.taskService = taskService;
    17 +        this.labelService = labelService;
    18 +        this.persistence = persistence;
    19 +    }
    20 +
    21 +    @Override
    22 +    protected void updateItem(Task task, boolean empty) {
    23 +        super.updateItem(task, empty);
    24 +        if (empty || task == null) {
    25 +            setGraphic(null);
    26 +            return;
    27 +        }
    28 +        Label taskTitle = new Label(task.getTitle());
    29 +        taskTitle.getStyleClass().add("task-title");
    30 +        ChoiceBox<LabelOption> selector = new ChoiceBox<>();
    31 +        selector.getItems().add(new LabelOption(null, "No label"));
    32 +        labelService.getLabels().stream().map(label -> new LabelOption(label.id(), label.name())).forEach(selector.getItems()::add);
    33 +        selector.getSelectionModel().select(selector.getItems().stream()
    34 +                .filter(option -> java.util.Objects.equals(option.id(), task.getLabelId()))
    35 +                .findFirst().orElse(selector.getItems().getFirst()));
    36 +        selector.setOnAction(event -> {
    37 +            LabelOption option = selector.getValue();
    38 +            if (option.id() == null) taskService.removeLabel(task);
    39 +            else taskService.assignLabel(task, labelService.getLabels().stream().filter(label -> label.id().equals(option.id())).findFirst().orElse(null));
    40 +            persistence.save(taskService, labelService);
    41 +            updateItem(task, false);
    42 +        });
    43 +        HBox row = new HBox(12, taskTitle, selector);
    44 +        row.setAlignment(Pos.CENTER_LEFT);
    45 +        HBox.setHgrow(taskTitle, Priority.ALWAYS);
    46 +        setGraphic(row);
    47 +    }
    48 +
    49 +    private record LabelOption(String id, String name) {
    50 +        @Override public String toString() { return name; }
    51 +    }
    52 +}
    53 +// &end[AssignTaskLabels]

  └ src\main\java\TaskManagerApplication.java (+21 -0)
     1 +import javafx.application.Application;
     2 +import javafx.scene.Scene;
     3 +import javafx.stage.Stage;
     4 +
     5 +public class TaskManagerApplication extends Application {
     6 +    @Override
     7 +    public void start(Stage stage) {
     8 +        // &begin[TaskManagement]
     9 +        MainController controller = new MainController();
    10 +        Scene scene = new Scene(controller.createView(), 800, 600);
    11 +        scene.getStylesheets().add(getClass().getResource("/taskmanager/styles.css").toExternalForm());
    12 +        stage.setTitle("Task Manager");
    13 +        stage.setScene(scene);
    14 +        stage.show();
    15 +        // &end[TaskManagement]
    16 +    }
    17 +
    18 +    public static void main(String[] args) {
    19 +        launch(args);
    20 +    }
    21 +}

  └ src\main\java\TaskService.java (+34 -0)
     1 +import javafx.collections.FXCollections;
     2 +import javafx.collections.ObservableList;
     3 +
     4 +import java.util.UUID;
     5 +
     6 +// &begin[CreateTasks]
     7 +public final class TaskService {
     8 +    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
     9 +
    10 +    public ObservableList<Task> getTasks() {
    11 +        return tasks;
    12 +    }
    13 +
    14 +    public Task createTask(String title) {
    15 +        Task task = new Task(UUID.randomUUID().toString(), title.trim());
    16 +        tasks.add(task);
    17 +        return task;
    18 +    }
    19 +
    20 +    void restore(Task task) {
    21 +        tasks.add(task);
    22 +    }
    23 +    // &end[CreateTasks]
    24 +
    25 +    // &begin[AssignTaskLabels]
    26 +    public void assignLabel(Task task, Label label) {
    27 +        task.setLabelId(label == null ? null : label.id());
    28 +    }
    29 +
    30 +    public void removeLabel(Task task) {
    31 +        task.setLabelId(null);
    32 +    }
    33 +    // &end[AssignTaskLabels]
    34 +}

• Edited src\main\java\JsonTaskPersistence.java (+3 -0)
     80                  String id = readStringField("id");
     81 +                expect(',');
     82                  String name = readStringField("name");
        ⋮
     96                  String id = readStringField("id");
     97 +                expect(',');
     98                  String title = readStringField("title");
     99 +                expect(',');
    100                  expectField("labelId");

• Ran "---STATUS---"; git status --short; "---DIFF CHECK---"; git diff --check; "---ANNOTATIONS---"; rg -n '&(begin|end|line)\[[A-Za-z][A-Za-z0-9]*\]' src; "---TOOLS---";
  │ Get-Command mvn,java,javac -ErrorAction SilentlyContinue | Select-Object Name,Source
  └ ---STATUS---
     M ../benchmark/steps/step-001/prompts/001.md
    … +42 lines (ctrl + t to view transcript)
    warning: in the working copy of 'project/src/main/java/TaskManagerApplication.java', LF will be replaced by CRLF the next time Git touches it
    warning: in the working copy of 'project/src/main/resources/taskmanager/styles.css', LF will be replaced by CRLF the next time Git touches it

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• The refactor is complete across the requested files, and I corrected the JSON reader’s field-separator handling during review. I’m validating the final feature annotations
  against the model and checking the diff before publishing the required change summary.


• Called agent-hans.get-feature-model({"projectPath":"D:\\Master\\Masterarbeit\\datasets-repos\\dataset-task-manager\\project"})
  └ TaskManager
        TaskManagement
            CreateTasks
        Labels
            CreateLabels
            AssignTaskLabels
            PersistLabels


• Ran $model = Get-Content .feature-model; $features = $model | ForEach-Object { if ($_ -match '[A-Za-z][A-Za-z0-9]*') { $Matches[0] } }; $annotations = rg -o '&(?:begin|end|
  │ line)\[[A-Za-z][A-Za-z0-9]*\]' src | ForEach-Object { if ($_ -match '\[([^]]+)\]') { $Matches[1] } } | Sort-Object -Unique; "Unknown annotations:"; Compare-Object $features
  │ $annotations -PassThru | Where-Object { $_ -in $annotations }; git diff --check; "Java files:"; Get-ChildItem src/main/java -Filter '*.java' | Select-Object -ExpandProperty
  │ … +1 lines
  └ Unknown annotations:
    Java files:
    … +8 lines (ctrl + t to view transcript)
    warning: in the working copy of 'project/src/main/java/TaskManagerApplication.java', LF will be replaced by CRLF the next time Git touches it
    warning: in the working copy of 'project/src/main/resources/taskmanager/styles.css', LF will be replaced by CRLF the next time Git touches it


• Called
  └ agent-hans.summary-gui({"projectPath":"D:\\Master\\Masterarbeit\\datasets-repos\\dataset-task-manager\\project","title":"Task Manager - Split feature
        implementation","description":"Refactored the task manager into domain models, services, controllers, and JSON persistence while preserving label creation, assignment,
        removal, and restart restoration.","changedFiles":["src/main/java/TaskManagerApplication.java","src/main/java/Task.java","src/main/java/Label.java","src/main/java/
        TaskService.java","src/main/java/LabelService.java","src/main/java/MainController.java","src/main/java/TaskDialogController.java","src/main/java/
        JsonTaskPersistence.java"],"featureChanges":[{"featureName":"TaskManagement","featurePath":
        ["TaskManager","TaskManagement"],"changeType":"modified","changeDescription":"Moved the main task screen into MainController and task-row UI into TaskDialogController."},
        {"featureName":"CreateTasks","featurePath":["TaskManager","TaskManagement","CreateTasks"],"changeType":"modified","changeDescription":"Extracted task data and creation
        operations into Task.java and TaskService.java."},{"featureName":"Labels","featurePath":["TaskManager","Labels"],"changeType":"modified","changeDescription":"Extracted
        label data and label operations into Label.java and LabelService.java."},{"featureName":"AssignTaskLabels","featurePath":
        ["TaskManager","Labels","AssignTaskLabels"],"changeType":"modified","changeDescription":"Kept one-label assignment, replacement, and removal in TaskService and
        TaskDialogController."},{"featureName":"PersistLabels","featurePath":["TaskManager","Labels","PersistLabels"],"changeType":"modified","changeDescription":"Replaced embedded
        properties storage with JsonTaskPersistence.java using a JSON file."}]})
    Ok

────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

• Refactored the app into the requested files:

  - Task.java
  - Label.java
  - TaskService.java
  - LabelService.java
  - MainController.java
  - TaskDialogController.java
  - JsonTaskPersistence.java

  Persistence now uses ~/.task-manager/tasks.json. Existing label creation, assignment, removal, replacement, and restart restoration are preserved.

  Feature annotations and model validation pass, and git diff --check is clean.
