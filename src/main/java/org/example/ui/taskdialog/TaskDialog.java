package org.example.ui.taskdialog;

import net.miginfocom.swing.MigLayout;
import org.example.AppMessages;
import org.example.AppTheme;
import org.example.EntryType;
import org.example.Task;
import org.example.TaskDao;
import org.example.TaskHierarchyUtil;
import org.example.TaskService;
import org.example.UiLabels;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Frame;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 【タスク追加・編集用ダイアログクラス】
 * タスク名、担当者、開始日・終了日、進捗率を入力するためのモーダル画面です。
 */
public class TaskDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final TaskService taskService;

    private static final String TYPE_PROJECT = EntryType.PROJECT.name();
    private static final String TYPE_PHASE = EntryType.PHASE.name();
    private static final String TYPE_TASK = EntryType.TASK.name();

    private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{TYPE_PROJECT, TYPE_PHASE, TYPE_TASK});
    private final JTextField nameField = new JTextField(20);
    private final JTextField assigneeField = new JTextField(20);
    private final JTextField startDateField = new JTextField(10);
    private final JTextField endDateField = new JTextField(10);
    private final JComboBox<String> priorityCombo = new JComboBox<>(Task.PRIORITY_OPTIONS);
    private final JSlider progressSlider = new JSlider(0, 100, 0);
    private final JLabel progressLabel = new JLabel("0%");
    private final JComboBox<String> parentProjectCombo = new JComboBox<>();
    private final Map<String, Task> parentTargetMap = new LinkedHashMap<>();

    private boolean confirmed = false;

    public TaskDialog(Frame parent, TaskService taskService) {
        this(parent, taskService, null, null, null);
    }

    public TaskDialog(Frame parent, TaskService taskService, String defaultType, Integer defaultParentProjectId) {
        this(parent, taskService, null, defaultType, defaultParentProjectId);
    }

    public TaskDialog(Frame parent, TaskService taskService, Task task) {
        this(parent, taskService, task, null, null);
    }

    private TaskDialog(Frame parent, TaskService taskService, Task task, String defaultType, Integer defaultParentProjectId) {
        super(parent, TaskDialogTitles.forTask(task), true);
        this.taskService = taskService != null ? taskService : new TaskService(new TaskDao());
        setSize(470, 430);
        setLocationRelativeTo(parent);
        setLayout(new MigLayout("fill, insets 0, gap 0", "[grow]", "[grow][]"));

        AppTheme.styleTextField(nameField);
        AppTheme.styleTextField(assigneeField);
        AppTheme.styleTextField(startDateField);
        AppTheme.styleTextField(endDateField);

        TaskDialogUiStyle.styleComboBox(typeCombo);
        TaskDialogUiStyle.styleComboBox(parentProjectCombo);
        TaskDialogUiStyle.styleComboBox(priorityCombo);
        UiLabels.installPriorityRenderer(priorityCombo);
        typeCombo.setRenderer(TaskDialogUiStyle.createTypeRenderer());

        progressSlider.setOpaque(false);
        progressSlider.setForeground(AppTheme.isDarkMode() ? new Color(96, 165, 250) : AppTheme.PRIMARY);
        progressLabel.setFont(AppTheme.FONT_MAIN);
        progressLabel.setForeground(AppTheme.isDarkMode() ? new Color(226, 232, 240) : AppTheme.TEXT_PRIMARY);

        parentProjectCombo.setEnabled(task == null);
        if (task == null) {
            typeCombo.setSelectedItem(defaultType != null ? EntryType.fromString(defaultType).name() : TYPE_PHASE);
        } else {
            if (task.getLevel() == 1 || task.getParentId() == null) {
                typeCombo.setSelectedItem(TYPE_PROJECT);
            } else if (task.getLevel() == 2) {
                typeCombo.setSelectedItem(TYPE_PHASE);
            } else {
                typeCombo.setSelectedItem(TYPE_TASK);
            }
            typeCombo.setEnabled(false);
        }

        typeCombo.addActionListener(e -> updateParentProjectVisibility());
        updateParentProjectVisibility();
        TaskDialogParentSupport.applyDefaultSelection(
                parentProjectCombo, parentTargetMap,
                (String) typeCombo.getSelectedItem(),
                defaultParentProjectId,
                taskService.getAllTasks());

        LocalDate today = LocalDate.now();
        startDateField.setText(today.toString());
        endDateField.setText(today.plusDays(7).toString());

        TaskDialogFormFactory.FormParts form = TaskDialogFormFactory.buildForm(
                typeCombo, parentProjectCombo, nameField, assigneeField,
                startDateField, endDateField, priorityCombo, progressSlider, progressLabel,
                parent);
        JPanel buttonPanel = TaskDialogFormFactory.buildButtonPanel(this::handleSave, this::dispose);

        if (task != null) {
            nameField.setText(task.getName());
            assigneeField.setText(task.getAssignee());
            if (task.getStartDate() != null) {
                startDateField.setText(task.getStartDate().toString());
            }
            if (task.getEndDate() != null) {
                endDateField.setText(task.getEndDate().toString());
            }
            priorityCombo.setSelectedItem(task.getPriorityCode());
            progressSlider.setValue(task.getProgress());
            progressLabel.setText(task.getProgress() + "%");
            if (task.getParentId() != null) {
                Task rootProject = TaskHierarchyUtil.findRootProject(task, this.taskService.getAllTasks());
                if (rootProject != null) {
                    parentProjectCombo.setSelectedItem(rootProject.getName());
                }
            }
        } else {
            priorityCombo.setSelectedItem(Task.DEFAULT_PRIORITY);
        }

        add(form.formPanel(), "grow, wrap");
        add(buttonPanel, "grow");
        setBackground(AppTheme.BACKGROUND);
    }

    private void updateParentProjectVisibility() {
        List<Task> allTasks = taskService.getAllTasks();
        parentTargetMap.clear();
        parentTargetMap.putAll(TaskDialogParentSupport.buildOptions(
                (String) typeCombo.getSelectedItem(), allTasks));
        TaskDialogParentSupport.applyModel(parentProjectCombo, parentTargetMap);
        TaskDialogParentSupport.syncVisibility(typeCombo, parentProjectCombo, parentTargetMap);
    }

    private void handleSave() {
        TaskDialogSaveValidator.Result result = TaskDialogSaveValidator.validate(
                nameField.getText(),
                (String) typeCombo.getSelectedItem(),
                getParentProjectId(),
                startDateField.getText(),
                endDateField.getText());
        if (!result.ok()) {
            JOptionPane.showMessageDialog(this,
                    result.message(),
                    AppMessages.get("dialog.error.input", "入力エラー"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getEntryType() {
        return (String) typeCombo.getSelectedItem();
    }

    public void setEntryType(String type) {
        typeCombo.setSelectedItem(EntryType.fromString(type).name());
    }

    public String getTaskName() {
        return nameField.getText().trim();
    }

    public void setTaskName(String name) {
        nameField.setText(name);
    }

    public String getAssignee() {
        return assigneeField.getText().trim();
    }

    public void setAssignee(String assignee) {
        assigneeField.setText(assignee);
    }

    public String getStartDate() {
        return startDateField.getText().trim();
    }

    public void setStartDate(String startDate) {
        startDateField.setText(startDate);
    }

    public String getEndDate() {
        return endDateField.getText().trim();
    }

    public void setEndDate(String endDate) {
        endDateField.setText(endDate);
    }

    public String getPriority() {
        return (String) priorityCombo.getSelectedItem();
    }

    public void setPriority(String priority) {
        priorityCombo.setSelectedItem(priority);
    }

    public int getProgress() {
        return progressSlider.getValue();
    }

    public void setProgress(int progress) {
        progressSlider.setValue(progress);
        progressLabel.setText(progress + "%");
    }

    public Integer getParentProjectId() {
        Task selectedParent = TaskDialogParentSupport.selectedParent(parentProjectCombo, parentTargetMap);
        return TaskDialogParentSupport.rootProjectId(selectedParent, taskService.getAllTasks());
    }

    public Integer getParentTaskId() {
        Task selectedParent = TaskDialogParentSupport.selectedParent(parentProjectCombo, parentTargetMap);
        return selectedParent != null ? selectedParent.getId() : null;
    }
}
