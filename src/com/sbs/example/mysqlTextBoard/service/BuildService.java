package com.sbs.example.mysqlTextBoard.service;

import java.util.List;

import com.sbs.example.mysqlTextBoard.container.Container;
import com.sbs.example.mysqlTextBoard.dto.Article;
import com.sbs.example.mysqlTextBoard.dto.Board;
import com.sbs.example.mysqlTextBoard.util.Util;

public class BuildService {
	ArticleService articleService;

	public BuildService() {
		articleService = Container.articleService;

	}

	public void builSite() {
		System.out.println("= site 폴더 생성 =");

		Util.rmdir("site"); // 기존 site 폴더 삭제
		Util.mkdir("site"); // 신규 site 폴더 생성

		// site_template에 있는 app.css(원본)를 복사해 site폴더 생성시 그 안에 복사본 붙여넣기
		Util.copy("site_template/app.css", "site/app.css");

		List<Article> articles = articleService.getArticlesForPrint();

		String head = getHeadHtml();
		String foot = Util.getFileContents("site_template/foot.html"); // foot.html 가져오기

		System.out.println("= article 상세페이지 생성 =");
		int articleIndex = 0;
		for (Article article : articles) {
			StringBuilder html = new StringBuilder();

			// 게시물 1개당 1개의 html 작성

			// head.html을 따로 생성해 위쪽에 붙임
			html.append(head);

			html.append("<div>");
			html.append("번호 : " + article.id + "<br>");
			html.append("작성일 : " + article.regDate + "<br>");
			html.append("수정일 : " + article.updateDate + "<br>");
			html.append("제목 : " + article.title + "<br>");
			html.append("작성자 : " + article.extra_memberName + "<br>");
			html.append("내용 : " + article.body + "<br>");
			if (articleIndex > 0) {
				html.append("<a href=\"" + (article.id - 1) + ".html" + "\">이전글</a><br>");
			}
			if (articleIndex < articles.size() - 1) {
				html.append("<a href=\"" + (article.id + 1) + ".html" + "\">다음글</a><br>");
			}
			html.append("</div>");

			// head.html과 마찬가지로 foot.html을 따로 생성해 아래쪽에 붙임
			html.append(foot);

			String fileName = article.id + ".html";
			String path = "site/" + fileName;

			Util.writeFile(path, html.toString());

			System.out.println(path + " 생성");
			articleIndex++;

		}
		System.out.println("= article 상세페이지 생성 종료 =");
	}

	private String getHeadHtml() {
		List<Board> boards = articleService.getBoards();

		String head = Util.getFileContents("site_template/head.html"); // head.html 가져오기
		StringBuilder boardMenuContentHtml = new StringBuilder();
		
		for (Board board : boards) {
			boardMenuContentHtml.append("<li>");

			String link = board.code + "-list-1.html";

			boardMenuContentHtml.append("<a href=\"" + link + "\" class=\"block\">");

			String iClass = "fas fa-clipboard-list"; // defult 아이콘

			if (board.code.contains("notice")) { // 공지사항 게시판 아이콘
				iClass = "fas fa-exclamation";
			} else if (board.code.contains("free")) { // 자유 게시판 아이콘
				iClass = "fas fa-users";
			}

			boardMenuContentHtml.append("<i class=\"" + iClass + "\"></i>");
			boardMenuContentHtml.append(" <span>" + board.name + "</span>");
			boardMenuContentHtml.append("</a>");

			boardMenuContentHtml.append("</li>");

		}

		head = head.replace("[게시판 이름 블록]", boardMenuContentHtml.toString());
		return head;
	}

}