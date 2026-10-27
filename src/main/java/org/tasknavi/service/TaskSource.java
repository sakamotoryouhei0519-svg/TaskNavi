package org.tasknavi.service;

/**
 * 「今日」と期限の通知が見る、プロジェクト 1 つ分のタスク。
 *
 * @param name    画面に出す名前（「自分だけのタスク」や共有プロジェクト名）
 * @param current 今選んでいるプロジェクトなら true
 * @param service そのプロジェクトのタスクを読み書きする入口
 */
public record TaskSource(String name, boolean current, TaskService service) {
}
