UTF-8

■概要
imotenはNTT Docomoの携帯宛てのメールをimode.net(有料サービス)を
経由して別のメールアドレスへ転送するプログラムです。

特徴
・Javaで書かれているのでマルチプラットフォーム
・バックグラウンドで動作
　Windowsではサービスで動作します。
・添付ファイル、デコメール対応
・絵文字の置き換え対応
　基本絵文字、拡張絵文字をGmailの絵文字画像に置き換えます。
　HTMLでリンクを張る方法と、送信時にダウンロードしたものを
　インラインで添付する方法があります。
　[晴れ]のような文字列に置き換える方法も選択できます。
　メールの題名部分は画像を使用できないので、文字列で置き換えます。


Windowsサービス、UNIXデーモンへの対応には
Java Service Wrapper(http://wrapper.tanukisoftware.org/)の
コミュニティ版を使用させてもらいました。


■インストール方法
　INSTALL.txt を確認してください。

■設定方法
　imoten.ini で設定します。
　パラメータについては Parameter.txt を確認してください。

■うまく動作しないとき
　詳細なログが出るように設定を変更して、ログを確認して下さい。
　logs/wrapper.log, logs/imoten.log にHTTP,SMTPの詳細な
　ログが出力されるようになります。

　▼imoten.ini
	mail.debug=true に変更します。

　▼log4j.properties
　　#log4j.rootLogger=DEBUG, file
　　log4j.rootLogger=INFO, file
　　　を
　　log4j.rootLogger=DEBUG, file
　　#log4j.rootLogger=INFO, file
　　　に変更します。

　ログファイルはローテートするので、ログでHDDがいっぱいに
　なることはありません。




