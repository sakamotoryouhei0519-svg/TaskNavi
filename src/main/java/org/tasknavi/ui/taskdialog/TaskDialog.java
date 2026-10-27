package org.tasknavi.ui.taskdialog;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.model.EntryType;
import org.tasknavi.model.Task;
import org.tasknavi.model.TaskBusinessRules;
import org.tasknavi.model.TaskHierarchyUtil;
import org.tasknavi.model.TaskStatus;
import org.tasknavi.service.TaskService;
import org.tasknavi.team.TeamTaskStore;
import org.tasknavi.ui.AppTheme;
import org.tasknavi.ui.DialogFit;
import org.tasknavi.ui.DialogKeys;
import org.tasknavi.ui.NoticeBar;
import org.tasknavi.ui.UiLabels;
import org.tasknavi.ui.team.TaskCommentsPanel;
import org.tasknavi.util.TaskStatusCycle;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * タスクを作る・直す窓。名前・担当者・期間・優先度・メモなどを入力する。
 */
public class TaskDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final List<Task> allTasks;

    private static final String TYPE_PROJECT = EntryType.PROJECT.name();
    private static final String TYPE_PHASE = EntryType.PHASE.name();
    private static final String TYPE_TASK = EntryType.TASK.name();

    private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{TYPE_PROJECT, TYPE_PHASE, TYPE_TASK});
    private final JTextField nameField = new JTextField(20);
    private final JTextField assigneeField = new JTextField(20);
    private final JTextField startDateField = new JTextField(10);
    private final JTextField endDateField = new JTextField(10);
    private final JComboBox<String> priorityCombo = new JComboBox<>(Task.PRIORITY_OPTIONS);
    private final JComboBox<String> statusCombo = new JComboBox<>(Task.STATUS_OPTIONS);
    private final JTextArea memoArea = new JTextArea(4, 20);
    private final boolean progressLocked;
    private int baselineProgress;
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
        this(parent, Objects.requireNonNull(taskService, "taskService").getAllTasks(),
                task, defaultType, defaultParentProjectId, taskService.assigneeOptions(), commentsFor(taskService, task));
    }

    /** コメントは共有プロジェクトの、すでにあるタスクだけ。 */
    private static JComponent commentsFor(TaskService taskService, Task task) {
        if (task != null && taskService.currentStore() instanceof TeamTaskStore store) {
            String uuid = store.uuidOf(task.getId());
            if (uuid != null) {
                return new TaskCommentsPanel(store.project(), uuid);
            }
        }
        return null;
    }

    /**
     * TaskService を通さない一覧で開く。
     *
     * @param task             編集するタスク。追加なら null
     * @param assigneeOptions  担当者欄の横のボタンから選べる名前。空ならボタンを出さない
     */
    public TaskDialog(Frame parent, List<Task> allTasks, Task task, String defaultType,
                      Integer defaultParentProjectId, List<String> assigneeOptions) {
        this(parent, allTasks, task, defaultType, defaultParentProjectId, assigneeOptions, null);
    }

    private TaskDialog(Frame parent, List<Task> allTasks, Task task, String defaultType,
                       Integer defaultParentProjectId, List<String> assigneeOptions, JComponent comments) {
        super(parent, TaskDialogTitles.forTask(task), true);
        this.allTasks = allTasks != null ? allTasks : List.of();
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

        TaskDialogUiStyle.styleComboBox(statusCombo);
        UiLabels.installStatusRenderer(statusCombo);
        progressLocked = task != null && TaskHierarchyUtil.hasChildren(task.getId(), allTasks);
        if (task != null) {
            baselineProgress = task.getProgress();
            statusCombo.setSelectedItem(TaskStatus.fromString(task.getStatusCode()).name());
        } else {
            baselineProgress = 0;
            statusCombo.setSelectedItem(Task.STATUS_NOT_STARTED);
        }
        statusCombo.setEnabled(!progressLocked);

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
                allTasks);

        LocalDate today = LocalDate.now();
        startDateField.setText(today.toString());
        endDateField.setText(today.plusDays(7).toString());

        TaskDialogFormFactory.FormParts form = TaskDialogFormFactory.buildForm(
                typeCombo, parentProjectCombo, nameField, assigneeField,
                startDateField, endDateField, priorityCombo, statusCombo,
                progressLocked
                        ? AppMessages.get("dialog.status.from.children", "子タスクの進捗から計算されます")
                        : null,
                memoArea,
                assigneeOptions,
                parent);
        TaskDialogFormFactory.DialogButtons buttons = TaskDialogFormFactory.buildButtonPanel(this::handleSave, this::dispose);

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
            memoArea.setText(task.getMemo());
            memoArea.setCaretPosition(0);
            if (task.getParentId() != null) {
                Task rootProject = TaskHierarchyUtil.findRootProject(task, allTasks);
                if (rootProject != null) {
                    parentProjectCombo.setSelectedItem(rootProject.getName());
                }
            }
        } else {
            priorityCombo.setSelectedItem(Task.DEFAULT_PRIORITY);
        }

        if (comments != null) {
            JLabel commentsLabel = new JLabel(AppMessages.get("dialog.label.comments", "コメント") + ":");
            commentsLabel.setForeground(AppTheme.TEXT_PRIMARY);
            form.formPanel().add(commentsLabel, "align right top, gaptop 6");
            form.formPanel().add(comments, "growx, wrap");
        }
        add(form.formPanel(), "grow, wrap");
        add(buttons.panel(), "grow");
        setBackground(AppTheme.BACKGROUND);
        widenFieldsToFitText();
        DialogFit.showAll(this);
        DialogKeys.install(this, buttons.saveButton(), this::dispose);
        getRootPane().registerKeyboardAction(
                e -> handleSave(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                nameField.requestFocusInWindow();
                nameField.selectAll();
            }
        });
    }

    private void widenFieldsToFitText() {
        FontMetrics metrics = parentProjectCombo.getFontMetrics(parentProjectCombo.getFont());
        int textWidth = metrics.stringWidth(nameField.getText());
        textWidth = Math.max(textWidth, metrics.stringWidth(assigneeField.getText()));
        for (int i = 0; i < parentProjectCombo.getItemCount(); i++) {
            Object item = parentProjectCombo.getItemAt(i);
            if (item != null) {
                textWidth = Math.max(textWidth, metrics.stringWidth(item.toString()));
            }
        }
        int fieldWidth = Math.max(280, textWidth + 64);
        Dimension fieldSize = new Dimension(fieldWidth, 36);
        typeCombo.setPreferredSize(fieldSize);
        parentProjectCombo.setPreferredSize(fieldSize);
        nameField.setPreferredSize(fieldSize);
        assigneeField.setPreferredSize(fieldSize);
        priorityCombo.setPreferredSize(fieldSize);
        statusCombo.setPreferredSize(fieldSize);
    }

    private void updateParentProjectVisibility() {
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
            NoticeBar.error(this, result.message());
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

    public String getMemo() {
        return memoArea.getText().strip();
    }

    public void setMemo(String memo) {
        memoArea.setText(memo);
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

    public String getStatus() {
        return (String) statusCombo.getSelectedItem();
    }

    public int getProgress() {
        if (progressLocked) {
            return baselineProgress;
        }
        return TaskBusinessRules.resolveProgress(getStatus(), baselineProgress);
    }

    public void setProgress(int progress) {
        baselineProgress = progress;
        statusCombo.setSelectedItem(TaskStatusCycle.fromProgress(progress));
    }

    public Integer getParentProjectId() {
        Task selectedParent = TaskDialogParentSupport.selectedParent(parentProjectCombo, parentTargetMap);
        return TaskDialogParentSupport.rootProjectId(selectedParent, allTasks);
    }

    public Integer getParentTaskId() {
        Task selectedParent = TaskDialogParentSupport.selectedParent(parentProjectCombo, parentTargetMap);
        return selectedParent != null ? selectedParent.getId() : null;
    }
}
