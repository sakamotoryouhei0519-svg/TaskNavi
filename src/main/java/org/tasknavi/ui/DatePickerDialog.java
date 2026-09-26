package org.tasknavi.ui;

import net.miginfocom.swing.MigLayout;
import org.tasknavi.AppMessages;
import org.tasknavi.AppTheme;
import org.tasknavi.UiLabels;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.time.LocalDate;

/**
 * 【日付選択ダイアログクラス】
 * カレンダー形式で日付を選択するためのモーダルダイアログです。
 * 
 * 【改善点】
 * TaskDialogからカレンダー機能を分離することで、コードの再利用性を高めました。
 * 他の画面でも同じカレンダー機能を使えるようになります。
 * 
 * 【重要単語の解説】
 * - モーダルダイアログ: 親画面を操作できなくするポップアップ画面
 * - 再利用性: 一度作ったコードを別の場所でも使い回せる性質
 * - 分離: 大きなクラスを複数の小さなクラスに分ける設計手法
 */
public class DatePickerDialog extends JDialog {
    // ==========================================
    //  【フィールド（プロパティ）】
    // ==========================================
    // private static final long serialVersionUIDとは、シリアル化（保存・復元）のためのID
    // シリアル化とは、オブジェクトをファイルに保存したりネットワークで送ったりできるように変換すること
    private static final long serialVersionUID = 1L;
    
    // LocalDate[]とは、LocalDateの配列（複数の日付を入れる箱）
    // 配列を1つだけ作って、その中身を書き換えることで、メソッド間で値を共有するテクニック
    // これは「参照渡し」という仕組みを利用しています
    private final LocalDate[] selectedDate;
    
    // JTextFieldとは、テキスト入力欄のクラス
    // finalをつけることで、このフィールド自体の書き換えを防いでいます（中身のテキストは変更可能）
    private final JTextField targetField;

    /**
     * 【コンストラクタ】
     * ダイアログを作成し、初期設定を行います。
     * 
     * @param parent 親ウィンドウ（このダイアログを開く元の画面）
     * @param targetField 選択した日付を設定するテキストフィールド
     * 
     * 【初心者向け解説】
     * コンストラクタとは、newでクラスを作る時に自動的に呼ばれる初期化メソッドです。
     * ここでは「どの画面の」「どの入力欄」に日付を設定するかを受け取っています。
     */
    public DatePickerDialog(Frame parent, JTextField targetField) {
        // super(parent, ...)とは、親クラス（JDialog）のコンストラクタを呼び出すこと
        // タイトルはダイアログのタイトル、trueはモーダル（親画面操作不可）を意味します
        super(parent, AppMessages.get("datepicker.title", "日付を選択"), true);
        
        this.targetField = targetField;
        
        // 配列を1つだけ作って、現在の日付を初期値として設定
        // 配列を使うのは、ラムダ式の中で値を書き換える必要があるためです
        this.selectedDate = new LocalDate[]{LocalDate.now()};
        
        // ダイアログの基本設定
        initializeDialog();
        
        // カレンダーUIの構築
        buildCalendarUI();
        
        // ダイアログを表示
        setVisible(true);
    }

    /**
     * 【ダイアログの初期化メソッド】
     * ダイアログのサイズ、配置、レイアウトなどの基本設定を行います。
     * 
     * 【改善点】
     * コンストラクタから初期化処理を分離することで、コードの可読性を向上させました。
     * 「何をしているか」が一目でわかるようになります。
     * 
     * 【初心者向け解説】
     * メソッド分割のメリット:
     * - コンストラクタが長くなりすぎない
     * - 処理の意味ごとにグループ化できる
     * - 後で修正しやすくなる
     */
    private void initializeDialog() {
        // setSizeとは、ウィンドウのサイズを設定すること（幅320、高さ280）
        setSize(320, 280);
        
        // setLocationRelativeTo(parent)とは、親ウィンドウの中央に配置すること
        // nullを指定すると画面の中央に配置されます
        setLocationRelativeTo(getParent());
        
        // setLayout(new MigLayout(...))とは、部品を配置するレイアウトを設定すること
        setLayout(new MigLayout("fill, insets 5, gap 5", "[grow]", "[][grow]"));
        
        // 背景色をテーマに合わせて設定
        setBackground(AppTheme.PANEL_BG);
    }

    /**
     * 【カレンダーUIの構築メソッド】
     * カレンダーの見た目を作成し、ダイアログに配置します。
     * 
     * 【初心者向け解説】
     * UI構築の手順:
     * 1. ヘッダー（年月表示と移動ボタン）を作る
     * 2. カレンダー本体（日付ボタン）を作る
     * 3. それらをダイアログに配置する
     */
    private void buildCalendarUI() {
        // ==========================================
        //  【ヘッダーパネルの作成】
        // ==========================================
        // JPanelとは、画面部品を乗せる板（パネル）のこと
        // MigLayoutで中央揃え、水平間隔10、垂直間隔5で配置
        JPanel headerPanel = new JPanel(new MigLayout("insets 0, gap 10, align center", "[][][]"));
        headerPanel.setBackground(AppTheme.PANEL_BG);
        
        // 前月ボタン
        JButton btnPrev = new JButton(AppMessages.get("calendar.button.prev", "＜"));
        styleNavigationButton(btnPrev);
        
        // 年月表示ラベル
        JLabel lblYearMonth = new JLabel("", SwingConstants.CENTER);
        lblYearMonth.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblYearMonth.setForeground(AppTheme.TEXT_PRIMARY);
        
        // 次月ボタン
        JButton btnNext = new JButton(AppMessages.get("calendar.button.next", "＞"));
        styleNavigationButton(btnNext);
        
        // ヘッダーに部品を追加
        headerPanel.add(btnPrev);
        headerPanel.add(lblYearMonth);
        headerPanel.add(btnNext);
        
        // ==========================================
        //  【カレンダーパネルの作成】
        // ==========================================
        // MigLayoutで7列のグリッドレイアウト、部品間の間隔2
        JPanel calendarPanel = new JPanel(new MigLayout("fill, insets 0, gap 2, wrap", "[][][][][][][]"));
        calendarPanel.setBackground(AppTheme.PANEL_BG);
        
        // ==========================================
        //  【カレンダー更新処理の定義】
        // ==========================================
        // Runnableとは、実行可能な処理を表すインターフェース
        // ラムダ式 () -> {} で処理の内容を简潔に記述できます
        // これを変数に入れることで、ボタンクリック時などに何度も呼び出せるようにしています
        Runnable updateCalendar = () -> {
            // カレンダーパネルを一旦クリア（前の表示を消す）
            calendarPanel.removeAll();
            
            // 年月ラベルを更新（例: "2024年 1月"）
            lblYearMonth.setText(selectedDate[0].format(java.time.format.DateTimeFormatter.ofPattern(
                    AppMessages.get("datepicker.date.format.yearmonth", "yyyy年 M月"))));
            
            // 曜日ラベルの追加（日・月・火・水・木・金・土）
            String[] days = UiLabels.weekdaysSundayFirst();
            for (int i = 0; i < 7; i++) {
                JLabel lblDay = new JLabel(days[i], SwingConstants.CENTER);
                lblDay.setFont(new Font("SansSerif", Font.BOLD, 12));
                // 日曜日は赤、土曜日は青で色分け（テーマ対応）
                if (i == 0) {
                    lblDay.setForeground(AppTheme.isDarkMode() ? new Color(248, 113, 113) : new Color(220, 38, 38));
                } else if (i == 6) {
                    lblDay.setForeground(AppTheme.isDarkMode() ? new Color(96, 165, 250) : new Color(37, 99, 235));
                } else {
                    lblDay.setForeground(AppTheme.TEXT_PRIMARY);
                }
                calendarPanel.add(lblDay);
            }
            
            // ==========================================
            //  【日付ボタンの追加】
            // ==========================================
            // withDayOfMonth(1)とは、その月の1日を取得すること
            LocalDate firstOfMonth = selectedDate[0].withDayOfMonth(1);
            
            // getDayOfWeek().getValue()とは、曜日を数値で取得（月曜=1, 日曜=7）
            // % 7 は、日曜日を0として扱うための計算
            int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
            
            // lengthOfMonth()とは、その月の日数（28〜31）を取得
            int lengthOfMonth = selectedDate[0].lengthOfMonth();
            
            // 最初の週の空白セルを追加（例: 1日が水曜日なら、日・月・火の分の空白を入れる）
            for (int i = 0; i < startDayOfWeek; i++) {
                calendarPanel.add(new JLabel(""));
            }
            
            // 日付ボタンを1日から月末まで追加
            for (int day = 1; day <= lengthOfMonth; day++) {
                // final int selectedDay = day; は、ラムダ式内で使うために変数をfinal扱いにするテクニック
                // ラムダ式内で使うローカル変数は、実質的にfinal（変更不可）である必要があります
                final int selectedDay = day;
                
                JButton btnDay = new JButton(String.valueOf(day));
                btnDay.setMargin(new Insets(2, 2, 2, 2));
                btnDay.setFocusPainted(false);
                btnDay.setForeground(AppTheme.TEXT_PRIMARY);
                
                // 今日の日付なら背景色を変える（テーマ対応）
                if (selectedDate[0].getDayOfMonth() == day) {
                    btnDay.setBackground(AppTheme.isDarkMode() ? new Color(59, 130, 246) : new Color(200, 220, 255));
                    btnDay.setForeground(Color.WHITE);
                } else {
                    btnDay.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
                }
                
                // 日付ボタンクリック時の処理
                btnDay.addActionListener(e -> {
                    // withDayOfMonth(selectedDay)とは、選択した日を含むLocalDateを作成
                    LocalDate date = selectedDate[0].withDayOfMonth(selectedDay);
                    
                    // テキストフィールドに日付を設定（YYYY-MM-DD形式）
                    targetField.setText(date.toString());
                    
                    // dispose()とは、ダイアログを閉じてリソースを解放すること
                    dispose();
                });
                
                calendarPanel.add(btnDay);
            }
            
            // revalidate()とは、レイアウトを再計算すること
            // repaint()とは、画面を再描画すること
            // この2つを呼ぶことで、カレンダーの表示が更新されます
            calendarPanel.revalidate();
            calendarPanel.repaint();
        };
        
        // ==========================================
        //  【ボタンイベントの設定】
        // ==========================================
        // 前月ボタンクリック時
        btnPrev.addActionListener(e -> {
            // minusMonths(1)とは、1ヶ月前の日付を取得
            selectedDate[0] = selectedDate[0].minusMonths(1);
            // カレンダーを再描画
            updateCalendar.run();
        });
        
        // 次月ボタンクリック時
        btnNext.addActionListener(e -> {
            // plusMonths(1)とは、1ヶ月後の日付を取得
            selectedDate[0] = selectedDate[0].plusMonths(1);
            // カレンダーを再描画
            updateCalendar.run();
        });
        
        // 初期カレンダーを描画
        updateCalendar.run();
        
        // ==========================================
        //  【ダイアログへの配置】
        // ==========================================
        // add(部品, 配置場所)で、部品をダイアログに追加
        add(headerPanel, "grow, wrap");
        add(calendarPanel, "grow");
    }

    /**
     * 【移動ボタンのスタイリングメソッド】
     * ナビゲーションボタン（前月・次月）の見た目を統一します。
     * 
     * 【改善点】
     * ボタンのスタイリングをメソッド化することで、コードの重複を削減しました。
     * 同じスタイルを複数のボタンに適用する際に便利です。
     * 
     * 【初心者向け解説】
     * メソッド化のメリット:
     * - 同じコードを何度も書かなくて済む
     * - スタイルを変更する時、このメソッド1つを変えるだけですむ
     * - コードが読みやすくなる
     * 
     * @param button スタイリング対象のボタン
     */
    private void styleNavigationButton(JButton button) {
        button.setFont(AppTheme.FONT_MAIN);
        button.setBackground(AppTheme.isDarkMode() ? new Color(30, 41, 59) : Color.WHITE);
        button.setForeground(AppTheme.TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
    }
}
