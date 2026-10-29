package org.tasknavi.ui.wbs;

import org.tasknavi.AppMessages;
import org.tasknavi.model.Task;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * タスク一覧から WBS 用ツリーモデル（ルート＋ノード対応表）を組み立てる。
 */
public final class WbsTreeModelBuilder {

    private WbsTreeModelBuilder() {
    }

    public static final class Result {
        private final DefaultMutableTreeNode root;
        private final Map<DefaultMutableTreeNode, Task> nodeTaskMap;

        public Result(DefaultMutableTreeNode root, Map<DefaultMutableTreeNode, Task> nodeTaskMap) {
            this.root = root;
            this.nodeTaskMap = nodeTaskMap;
        }

        public DefaultMutableTreeNode getRoot() {
            return root;
        }

        public Map<DefaultMutableTreeNode, Task> getNodeTaskMap() {
            return nodeTaskMap;
        }
    }

    public static Result build(
            List<Task> baseTasks,
            Integer filterProjectId,
            String searchKeyword,
            String statusFilter) {
        List<Task> source = baseTasks != null ? baseTasks : new ArrayList<>();

        Map<Integer, Task> taskMap = new HashMap<>();
        for (Task t : source) {
            taskMap.put(t.getId(), t);
        }

        Set<Integer> allowedTaskIds = new HashSet<>();
        for (Task task : source) {
            if (filterProjectId == null || WbsTreeFilter.isDescendantOrSelf(task, filterProjectId, taskMap)) {
                allowedTaskIds.add(task.getId());
            }
        }

        List<Task> projectFilteredTasks = new ArrayList<>();
        for (Task task : source) {
            if (allowedTaskIds.contains(task.getId())) {
                projectFilteredTasks.add(task);
            }
        }

        String keyword = (searchKeyword != null && !searchKeyword.trim().isEmpty()) ? searchKeyword : null;
        String status = (AppMessages.isFilterAll(statusFilter) || statusFilter == null) ? null : statusFilter;

        List<Task> allTasks;
        if (keyword != null || status != null) {
            allTasks = WbsTreeFilter.filterKeepingAncestors(projectFilteredTasks, keyword, status, taskMap);
        } else {
            allTasks = projectFilteredTasks;
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                AppMessages.get("wbs.tree.root", "全タスクプロジェクト"));
        Map<DefaultMutableTreeNode, Task> nodeTaskMap = new HashMap<>();

        taskMap.clear();
        for (Task t : allTasks) {
            taskMap.put(t.getId(), t);
        }

        Map<Integer, List<Task>> childrenMap = new HashMap<>();
        List<Task> rootTasks = new ArrayList<>();

        for (Task task : allTasks) {
            if (task.getParentId() == null || !taskMap.containsKey(task.getParentId())) {
                rootTasks.add(task);
            } else {
                childrenMap.computeIfAbsent(task.getParentId(), k -> new ArrayList<>()).add(task);
            }
        }

        int projectIndex = 1;
        for (Task rootTask : rootTasks) {
            DefaultMutableTreeNode projectNode = buildTreeNodeRecursive(
                    rootTask, String.valueOf(projectIndex), childrenMap, nodeTaskMap);
            root.add(projectNode);
            projectIndex++;
        }

        return new Result(root, nodeTaskMap);
    }

    /** 親の番号に、兄弟の中で何番目か（1 から）をつなげる。いちばん上は親の番号を空で渡す。 */
    static String childCode(String parentCode, int childIndex) {
        return parentCode == null || parentCode.isEmpty() ? String.valueOf(childIndex) : parentCode + "." + childIndex;
    }

    private static DefaultMutableTreeNode buildTreeNodeRecursive(
            Task currentTask,
            String code,
            Map<Integer, List<Task>> childrenMap,
            Map<DefaultMutableTreeNode, Task> nodeTaskMap) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(code + " " + currentTask.getName());
        nodeTaskMap.put(node, currentTask);

        List<Task> children = childrenMap.get(currentTask.getId());
        if (children != null) {
            int childIndex = 1;
            for (Task child : children) {
                node.add(buildTreeNodeRecursive(child, childCode(code, childIndex), childrenMap, nodeTaskMap));
                childIndex++;
            }
        }
        return node;
    }
}
