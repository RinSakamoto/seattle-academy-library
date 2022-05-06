package jp.co.seattle.library.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jp.co.seattle.library.dto.BookDetailsInfo;
import jp.co.seattle.library.service.BooksService;

/**
 * Handles requests for the application home page.
 */
@Controller // APIの入り口
public class BulkregistController {
	final static Logger logger = LoggerFactory.getLogger(BulkregistController.class);

	@Autowired
	private BooksService booksService;
	
	/**
	* 一括登録画面に遷移する
	*
	* @param model モデル
	* @return 遷移先画面
	*/

	@RequestMapping(value = "/bulkregist", method = RequestMethod.GET) // value＝actionで指定したパラメータ
	// RequestParamでname属性を取得
	public String bulkregist(Model model) {
		return "bulkregist";
	}

	/**
	 * 書籍情報を一括登録する
	 * 
	 * @param locale    ロケール情報
	 * @param title     書籍名
	 * @param author    著者名
	 * @param publisher 出版社
	 * @param file      サムネイルファイル
	 * @param model     モデル
	 * @return 遷移先画面
	 */
	@Transactional
	@RequestMapping(value = "/bulkregist", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
	public String bulkregist(Locale locale, @RequestParam("csv") MultipartFile file, Model model) {

		List<String> bulkerrorMessages = new ArrayList<String>();
		List<BookDetailsInfo> bookList = new ArrayList<BookDetailsInfo>();

		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

			String inputValue;
			int lineCount = 0;

			while ((inputValue = br.readLine()) != null) {
				String[] inputValues = inputValue.split(",", -1);

				BookDetailsInfo bookInfo = new BookDetailsInfo();
				bookInfo.setTitle(inputValues[0]);
				bookInfo.setAuthor(inputValues[1]);
				bookInfo.setPublisher(inputValues[2]);
				bookInfo.setPublishDate(inputValues[3]);
				bookInfo.setIsbn(inputValues[4]);

				// 行数カウントインクリメント
				lineCount++;

				// TODO バリデーションチェック

				if (inputValue.isEmpty()) {
					bulkerrorMessages.add("CSVに書籍情報がありません。");
				}
				if (inputValues[0].equals("") || inputValues[1].equals("") || inputValues[2].equals("")
						|| !inputValues[3].matches("^[0-9]{4}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])$")
						|| !inputValues[4].equals("") && !inputValues[4].matches("^[0-9]{10}|[0-9]{13}+$/")) {
					bulkerrorMessages.add(lineCount + "行目の書籍でエラーが起きました。");
				} else {
					bookList.add(bookInfo);
				}
			}

			if (bulkerrorMessages.size() > 0) {
				model.addAttribute("errorMessages", bulkerrorMessages);
				return "bulkregist";

			} else {
				booksService.bulkregistBook(bookList);
			}

		} catch (IOException e) {
			bulkerrorMessages.add("ファイルが読み込めません。");
			model.addAttribute("errorMessages", bulkerrorMessages);
			return "bulkregist";
		}
		model.addAttribute("bookList", booksService.getBookList());
		return "home";
	}
}
