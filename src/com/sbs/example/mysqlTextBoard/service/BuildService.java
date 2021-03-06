package com.sbs.example.mysqlTextBoard.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sbs.example.mysqlTextBoard.container.Container;
import com.sbs.example.mysqlTextBoard.dto.Article;
import com.sbs.example.mysqlTextBoard.dto.Board;
import com.sbs.example.mysqlTextBoard.dto.Project;
import com.sbs.example.mysqlTextBoard.util.Util;

public class BuildService {
	MemberService memberService;
	ArticleService articleService;
	DiscusApiService discusApiService;
	GoogleAnalyticsApiService googleAnalyticsApiService;

	public BuildService() {
		memberService = Container.memberService;
		articleService = Container.articleService;
		discusApiService = Container.discusApiService;
		googleAnalyticsApiService = Container.googleAnalyticsApiService;
	}

	public void builSite() {
		System.out.println("= site 폴더 생성 =");

		// Util.rmdir("site"); // 기존 site 폴더 삭제
		Util.mkdir("site"); // 신규 site 폴더 생성

		// site_template에 있는 app.css(원본)를 복사해 site폴더 생성시 그 안에 복사본 붙여넣기
		Util.copy("site_template/app.css", "site/app.css");
		Util.copy("site_template/app.js", "site/app.js");

		// site_template에 있는 images(원본)폴더를 복사해 site폴더 생성시 그 안에 복사본 붙여넣기
		Util.copyDir("site_template/images", "site/images");

		// 사이트 생성 전 Discus Data 가져오기
		loadDataFromDiscus();
		// 사이트 생성 전 GoogleAnalytics4 Data 가져오기
		loadDataFromGA4();

		buildIndexPage(); // 인덱스 페이지 생성
		buildAboutPage(); // about 페이지 생성
		buildArticleListPages(); // 각 게시판 별 게시물리스트 페이지 생성
		buildArticleDetailPages(); // 게시판 별 게시물 상세페이지 생성
		buildStatisticsPage(); // statistics 페이지 생성
		buildSearchPage(); // 검색 페이지 생성

	}

	// about 페이지 생성
	private void buildAboutPage() {

		StringBuilder html = new StringBuilder();

		String head = getHeadHtml("about");
		head = head.replace("[메인 박스 섹션 태그 시작]", "");
		String foot = Util.getFileContents("site_template/foot.html");
		String mainHtml = Util.getFileContents("site_template/about.html");

		html.append(head);
		html.append(mainHtml);
		html.append(foot);

		String fileName = "about.html";
		String path = "site/" + fileName;

		Util.writeFile(path, html.toString());

	}

	// statistics 페이지 생성
	private void buildStatisticsPage() {

		StringBuilder html = new StringBuilder();

		String head = getHeadHtml("statistics");
		String mainBoxSectionStart = "<section class=\"main-box-section con-min-width\">";
		head = head.replace("[메인 박스 섹션 태그 시작]", mainBoxSectionStart);
		String foot = Util.getFileContents("site_template/foot.html");
		String mainHtml = Util.getFileContents("site_template/statistics.html");

		html.append(head);
		int visitorCount = memberService.getVisitorCount();
		int boardCount = articleService.getBoardCount();
		int articleCount = articleService.getArticleCount();

		StringBuilder body = new StringBuilder();
		body.append("<div class=\"count-box__visitor\">방문자 현황: " + visitorCount + "</div>");
		body.append("<div class=\"count-box__board\">게시판 현황: " + boardCount + "</div>");
		body.append("<div class=\"count-box__article\">게시글 현황: " + articleCount + "</div>");

		mainHtml = mainHtml.replace("[방문자, 게시판, 게시물 현황]", body.toString());
		html.append(mainHtml);
		html.append(foot);

		String fileName = "statistics.html";
		String path = "site/" + fileName;

		Util.writeFile(path, html.toString());

	}

	// search 페이지 생성
	private void buildSearchPage() {
		// 전체 게시물 불러오기
		List<Article> articles = articleService.getArticlesForPrint();

		// 전체 게시물 리스트를 json파일로 만들기
		String jsonText = Util.getJsonText(articles);
		Util.writeFile("site/article_list.json", jsonText);

		Util.copy("site_template/search.js", "site/search.js");

		StringBuilder html = new StringBuilder();

		String head = getHeadHtml("search");
		String mainBoxSectionStart = "<section class=\"main-box-section con-min-width\">";
		head = head.replace("[메인 박스 섹션 태그 시작]", mainBoxSectionStart);
		String foot = Util.getFileContents("site_template/foot.html");
		String mainHtml = Util.getFileContents("site_template/search.html");

		html.append(head);
		html.append(mainHtml);
		html.append(foot);

		String fileName = "search.html";
		String path = "site/" + fileName;

		Util.writeFile(path, html.toString());

	}

	// GoogleAnalytics4 Data 가져오기
	private void loadDataFromGA4() {
		googleAnalyticsApiService.updatePageHits();

	}

	// Discus Data 가져오기
	private void loadDataFromDiscus() {
		discusApiService.updateArticleCounts();
	}

	// 각 게시판 별 게시물리스트 페이지 생성
	private void buildArticleListPages() {

		System.out.println("= article 리스트 페이지 생성 =");

		List<Board> boards = articleService.getBoards();

		for (Board board : boards) {

			List<Article> articles = articleService.getBoardArticlesByCodeForPrint(board.getCode());

			Collections.reverse(articles); // 최신순 리스팅

			int articlesInAPage = 10; // 한 페이지에 들어갈 article 수 설정
			int pageMenuBoxSize = 5; // 한 메인페이지 화면에 나올 하단 페이지 메뉴 버튼 수 ex) 1 2 3 4 5 6 7 8 9 10
			int totalArticlesCount = articles.size(); // 전체 article의 수 카운팅
			int totalPages = (int) Math.ceil((double) totalArticlesCount / articlesInAPage); // 총 필요 페이지 수 카운팅

			// 각각의 페이지를 한개씩 반복적으로 생성
			// 총 필요 페이지 수까지
			for (int page = 1; page <= totalPages; page++) {
				buildArticleListPage(board, articles, articlesInAPage, pageMenuBoxSize, totalPages, page);
			}
		}

		System.out.println("= article 리스트 페이지 생성 종료 =");
	}

	// 게시판 페이지별 게시물 리스트 생성
	private void buildArticleListPage(Board board, List<Article> articles, int articlesInAPage, int pageMenuBoxSize,
			int totalPages, int page) {

		System.out.println("= " + board.getName() + " 리스트 페이지 생성 =");

		String head = "";

		if (board.getCode().contains("p_")) {
			head = getHeadHtml("project_list_" + board.getCode());
		} else {
			head = getHeadHtml("article_list_" + board.getCode());
		}

		String mainBoxSectionStart = "<section class=\"main-box-section con-min-width\">";
		head = head.replace("[메인 박스 섹션 태그 시작]", mainBoxSectionStart);
		String foot = Util.getFileContents("site_template/foot.html");

		StringBuilder html = new StringBuilder();

		String template = Util.getFileContents("site_template/list.html");

		html.append(head);

		// 1~10 11~20 21~30 ......
		int startPoint = (page - 1) * articlesInAPage; // page=1일때 index=0(즉,id = 1) 2 10(11) 3 20(21)
		int endPoint = startPoint + articlesInAPage - 1; // page=1일때 index0~9 -> id1~10

		if (endPoint >= articles.size() - 1) {
			endPoint = articles.size() - 1;
		}

		StringBuilder mainBody = new StringBuilder();
		for (int i = startPoint; i <= endPoint; i++) {
			Article article = articles.get(i);

			mainBody.append("<div>");
			mainBody.append("<div class=\"article-list__cell-id\">" + article.getId() + "</div>");
			mainBody.append("<div class=\"article-list__cell-reg-date\">" + article.getRegDate() + "</div>");
			mainBody.append("<div class=\"article-list__cell-writer\">" + article.getExtra_memberName() + "</a></div>");
			mainBody.append("<div class=\"article-list__cell-title\"><a href=\"article_detail_" + article.getId()
					+ ".html\" class=\"hover-underline\">" + article.getTitle() + "</a>");
			mainBody.append("<span>[" + article.getCommentsCount() + "]</span></div>");
			mainBody.append("<div class=\"article-list__cell-hitsCount\">" + article.getHitCount() + "</div>");
			mainBody.append("<div class=\"article-list__cell-likesCount\">" + article.getLikesCount() + "</div>");
			mainBody.append("</div>");
		}

		/* 하단 페이지 이동 버튼 메뉴 시작 */
		// 1. pageMenuBox내 시작 번호, 끝 번호 설정

		int previousPageNumCount = (page - 1) / pageMenuBoxSize; // 현재 페이지가 2이면 previousPageNumCount = 1/5
		int boxStartNum = pageMenuBoxSize * previousPageNumCount + 1; // 총 페이지 수 30이면 1~5 6~10 11~15
		int boxEndNum = pageMenuBoxSize + boxStartNum - 1;

		if (boxEndNum > totalPages) {
			boxEndNum = totalPages;
		}

		// 2. '이전','다음' 버튼 페이지 계산
		int boxStartNumBeforePage = boxStartNum - 1;
		if (boxStartNumBeforePage < 1) {
			boxStartNumBeforePage = 1;
		}
		int boxEndNumAfterPage = boxEndNum + 1;
		if (boxEndNumAfterPage > totalPages) {
			boxEndNumAfterPage = totalPages;
		}

		// 3. '이전','다음' 버튼 필요 유무 판별
		boolean boxStartNumBeforePageBtnNeedToShow = boxStartNumBeforePage != boxStartNum;
		boolean boxEndNumAfterPageBtnNeedToShow = boxEndNumAfterPage != boxEndNum;

		StringBuilder pageMenuBody = new StringBuilder();

		link(board, page);

		if (boxStartNumBeforePageBtnNeedToShow) {
			pageMenuBody.append("<li class=\"before-btn\"><a href=\"" + link(board, boxStartNumBeforePage)
					+ "\" class=\"flex flex-ai-c\"> &lt; 이전</a></li>");
		}

		for (int i = boxStartNum; i <= boxEndNum; i++) {
			String selectedPageNum = "";
			if (i == page) {
				selectedPageNum = "article-page-menu__link--selected";
			}
			pageMenuBody.append("<li><a href=\"" + link(board, i) + "\" class=\"page-btn flex flex-ai-c "
					+ selectedPageNum + "\">" + i + "</a></li>");
		}
		if (boxEndNumAfterPageBtnNeedToShow) {
			pageMenuBody.append("<li class=\"after-btn\"><a href=\"" + link(board, boxEndNumAfterPage)
					+ "\" class=\"flex flex-ai-c\">다음 &gt;</a></li>");
		}

		String bodyTemplate = template.replace("[게시물 리스트 블록]", mainBody); // list 템플릿에 mainBody 끼워넣고
		html.append(bodyTemplate.replace("[게시물 리스트 페이지메뉴 블록]", pageMenuBody)); // bodyTemplate에 다시 pageMenuBody 끼워넣기
		
		/* 하단 페이지 이동 버튼 메뉴 끝 */
		
		
		html.append(foot);

		String fileName = board.getCode() + "-list-" + page + ".html";

		Util.writeFile("site/" + fileName, html.toString());

		System.out.println("= " + board.getName() + " 리스트 " + page + "페이지 생성 완료 =");

	}

	private String link(Board board, int page) {
		return board.getCode() + "-list-" + page + ".html";

	}

	// index 페이지 생성
	private void buildIndexPage() {

		System.out.println("= INDEX 페이지 생성 =");

		String head = getHeadHtml("index");
		head = head.replace("[메인 박스 섹션 태그 시작]", "");
		String foot = Util.getFileContents("site_template/foot.html");
		String template = Util.getFileContents("site_template/index.html");

		Util.copy("site_template/index.js", "site/index.js");

		StringBuilder html = new StringBuilder();

		html.append(head);

		StringBuilder mainBody = new StringBuilder();
		List<Article> articlesInNotice = articleService.getBoardArticlesByCodeForPrint("notice");
		List<Article> articlesRecent = articleService.getArticlesExceptNotice();

		Collections.reverse(articlesInNotice);
		Collections.reverse(articlesRecent);

		for (int i = 0; i < 5; i++) {
			if (articlesInNotice.size() <= i) {
				continue;
			}
			Article article = articlesInNotice.get(i);
			mainBody.append("<div>");
			mainBody.append("<div class=\"home-list__cell-id\">" + article.getId() + "</div>");
			mainBody.append("<div class=\"home-list__cell-title\"><a href=\"article_detail_" + article.getId()
					+ ".html\" class=\"hover-underline\">" + article.getTitle() + "</a></div>");
			mainBody.append("</div>");
		}

		String mainHtml = template.replace("[인덱스 페이지 공지 리스트]", mainBody);
		StringBuilder secondBody = new StringBuilder();

		for (int i = 0; i < 5; i++) {
			if (articlesRecent.size() <= i) {
				continue;
			}
			Article article = articlesRecent.get(i);
			secondBody.append("<div>");
			secondBody.append("<div class=\"home-list__cell-id\">" + article.getId() + "</div>");
			secondBody.append("<div class=\"home-list__cell-title\"><a href=\"article_detail_" + article.getId()
					+ ".html\" class=\"hover-underline\">" + article.getTitle() + "</a></div>");
			secondBody.append("</div>");
		}

		mainHtml = mainHtml.replace("[인덱스 페이지 최신글 리스트]", secondBody);

		html.append(mainHtml);
		
		html.append(foot);

		String fileName = "index.html";
		String path = "site/" + fileName;

		Util.writeFile(path, html.toString());

		System.out.println("= INDEX 페이지 생성 종료 =");
	}

	// 게시판 별 게시물 상세페이지 생성
	private void buildArticleDetailPages() {
		List<Board> boards = articleService.getBoards();

		for (Board board : boards) {
			List<Article> articles = articleService.getBoardArticlesByCodeForPrint(board.getCode());
			int articlesSize = articles.size();

			if (articlesSize <= 0) {
				continue;
			}
			int beforeArticleIndex = 0;
			int x = beforeArticleIndex;
			int beforeArticleId = articles.get(x).getId();

			String foot = Util.getFileContents("site_template/foot.html");
			String template = Util.getFileContents("site_template/detail.html");

			System.out.println("= article 상세페이지 생성 =");

			for (Article article : articles) {
				String getArticleTags = articleService.getArticleTagsByArticleId(article.getId());
				String[] tags = getArticleTags.split(",");

				String head = getHeadHtml("article_detail", article);
				String mainBoxSectionStart = "<section class=\"main-box-section con-min-width\">";
				head = head.replace("[메인 박스 섹션 태그 시작]", mainBoxSectionStart);

				StringBuilder html = new StringBuilder();

				html.append(head);

				String articleBody = article.getBody();
				articleBody = articleBody.replaceAll("script", "t-script");

				StringBuilder body = new StringBuilder();

				body.append("<div class=\"article-detail-cell__board-name\">");
				body.append("<div>");
				body.append("<span>게시판 : </span><span>" + board.getName() + " 게시판</span>");
				body.append("</div>");
				body.append("</div>");
				body.append("<div class=\"article-detail-cell__id\">");
				body.append("<div>");
				body.append("<span>번호 : </span><span>" + article.getId() + "</span>");
				body.append("</div>");
				body.append("</div>");
				body.append("<div class=\"article-detail-cell__member-id\">");
				body.append("<div>");
				body.append("<span>작성자 : </span><span>" + article.getExtra_memberName() + "</span>");
				body.append("</div>");
				body.append("</div>");
				body.append("<div class=\"article-detail-cell__reg-date\">");
				body.append("<div>");
				body.append("<span>작성일 : </span><span>" + article.getRegDate() + "</span>");
				body.append("</div>");
				body.append("</div>");
				body.append("<div class=\"article-detail-cell__update-date\">");
				body.append("<div>");
				body.append("<span>수정일 : </span><span>" + article.getUpdateDate() + "</span>");
				body.append("</div>");
				body.append("</div><br>");
				body.append("<div class=\"article-detail-cell__title-contents flex flex-jc-fe\">");
				body.append("<div class=\"article-detail-cell__hitsCount\">");
				body.append("<div>");
				body.append("<span>조회수 : </span><span>" + article.getHitCount() + "</span>");
				body.append("</div>");
				body.append("</div>");
				body.append("<div class=\"article-detail-cell__likesCount\">");
				body.append("<div>");
				body.append("<span>추천수 : </span><span>" + article.getLikesCount() + "</span>");
				body.append("</div>");
				body.append("</div>");
				body.append("<div class=\"article-detail-cell__commentsCount\">");
				body.append("<div>");
				body.append("<span>댓글수 : </span><span>" + article.getCommentsCount() + "</span>");
				body.append("</div>");
				body.append("</div>");
				body.append("</div>");
				body.append("<div class=\"article-detail-cell__title\">");
				body.append("<div>");
				body.append("<span>제목 : </span><span>" + article.getTitle() + "</span>");
				body.append("</div>");
				body.append("</div>");

				// 구글 애드센스2

				body.append("<nav class=\"ad\">");
				body.append("<!-- 구글 애드센스2 -->");
				body.append(
						"<script async src=\"https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js\"></script>");
				body.append("<!-- 수평 반응형2 -->");
				body.append(
						"<ins class=\"adsbygoogle\" style=\"display: block\" data-ad-client=\"ca-pub-7996879977557531\" data-ad-slot=\"2278060237\" data-ad-format=\"auto\" data-full-width-responsive=\"true\"></ins>");
				body.append("<script>(adsbygoogle = window.adsbygoogle || []).push({});</script>");
				body.append("</nav>");

				body.append("<script type\"text/x-template\">");
				body.append(articleBody);
				body.append("</script>");
				body.append("<div class=\"article-detail-cell__body height-70p toast-ui-viewer\">");
				body.append("<div>");
				body.append("</div>");
				body.append("</div><br>");
				body.append("<div class=\"article-detail-cell__tag flex flex-wrap\">");

				for (int i = 0; i < tags.length; i++) {
					String tag = tags[i];
					if (tag.equals("없음")) {
						continue;
					}

					body.append("<nav># " + tag + "</nav>");
				}

				body.append("</div><br><br>");

				// 구글 애드센스1
				body.append("<nav class=\"ad\">");
				body.append("<!-- 구글 애드센스1 -->");
				body.append(
						"<script async src=\"https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js\"></script>");
				body.append("<!-- 수평 반응형1 -->");
				body.append(
						"<ins class=\"adsbygoogle\" style=\"display: block\" data-ad-client=\"ca-pub-7996879977557531\" data-ad-slot=\"6812185708\" data-ad-format=\"auto\" data-full-width-responsive=\"true\"></ins>");
				body.append("<script>(adsbygoogle = window.adsbygoogle || []).push({});</script>");
				body.append("</nav>");

				// discus에게 정확한 페이지 경로 알려주기
				String domainUrl = Container.appConfig.getSiteDomain();
				String pageUrl = getArticleFileName(article);

				
				/* 상세페이지 하단 메뉴 시작 */
				StringBuilder pageMenuBody = new StringBuilder();

				if (article.getId() > beforeArticleId) {
					pageMenuBody.append("<div class=\"./\"><a href=\"article_detail_" + articles.get(x - 1).getId()
							+ ".html" + "\" class=\"hover-underline\">&lt 이전글</a></div>");
				}

				pageMenuBody.append("<div class=\"./\"><i class=\"fas fa-th-list\"></i><a href=\"" + board.getCode()
						+ "-list-1.html" + "\" class=\"hover-underline\"> 목록 </a></div>");
				if (x < articlesSize - 1) {
					pageMenuBody.append("<div class=\"./\"><a href=\"article_detail_" + articles.get(x + 1).getId()
							+ ".html" + "\"class=\"hover-underline\">다음글 &gt</a></div>");
				}

				String bodyTemplate = template.replace("[상세페이지 블록]", body); // list 템플릿에 mainBody 끼워넣고
				bodyTemplate = bodyTemplate.replace("[사이트 도메인]", domainUrl);
				bodyTemplate = bodyTemplate.replace("[사이트 이름.html]", pageUrl);
				html.append(bodyTemplate.replace("[상세페이지 하단 메뉴 블록]", pageMenuBody)); // bodyTemplate에 다시 pageMenuBody
																						// 끼워넣기
				/* 상세페이지 하단 메뉴 끝 */
				
				html.append(foot);
				String fileName = getArticleFileName(article);
				String path = "site/" + fileName;
				Util.writeFile(path, html.toString());
				System.out.println(path + " 생성");
				x++;
				beforeArticleId = articles.get(x - 1).getId();
			}
			System.out.println("= article 상세페이지 생성 종료 =");
		}
	}

	// article 파일명 작성
	public String getArticleFileName(Article article) {
		return "article_detail_" + article.getId() + ".html";
	}

	// head.html 가져오기 오버라이드
	private String getHeadHtml(String pageName) { // 만약, getHeadHtml()에 pageName만 있으면 getHeadHtml(pageName, null)로 리턴
		return getHeadHtml(pageName, null);
	}

	// head.html 가져오기
	private String getHeadHtml(String pageName, Object object) {
		List<Board> boards = articleService.getBoards();

		String head = Util.getFileContents("site_template/head.html"); // head.html 가져오기
		StringBuilder boardMenuContentHtml = new StringBuilder();
		StringBuilder projectMenuContentHtml = new StringBuilder();

		for (Board board : boards) {

			if (board.getCode().contains("p_")) {
				projectMenuContentHtml.append("<li title=\"" + board.getCode().toUpperCase() + "\">");

				String link = board.getCode() + "-list-1.html";

				projectMenuContentHtml.append("<a href=\"" + link + "\" class=\"block\">");
				projectMenuContentHtml.append(getTitleBarContentByPageName("project_list_" + board.getCode()));
				projectMenuContentHtml.append("</a>");
				projectMenuContentHtml.append("</li>");

			} else {
				boardMenuContentHtml.append("<li title=\"" + board.getCode().toUpperCase() + "\">");

				String link = board.getCode() + "-list-1.html";

				boardMenuContentHtml.append("<a href=\"" + link + "\" class=\"block\">");
				boardMenuContentHtml.append(getTitleBarContentByPageName("article_list_" + board.getCode()));
				boardMenuContentHtml.append("</a>");
				boardMenuContentHtml.append("</li>");
			}
		}

		head = head.replace("[게시판 이름 블록]", boardMenuContentHtml.toString());
		head = head.replace("[프로젝트 이름 블록]", projectMenuContentHtml.toString());

		// 입력받은 pageName에 맞는 타이틀바 컨텐츠를 리턴
		String titleBarContentHtml = getTitleBarContentByPageName(pageName);
		head = head.replace("[타이틀바 컨텐츠]", titleBarContentHtml);

		// 입력받은 pageName에 맞는 페이지의 타이틀을 리턴
		String pageTitle = getPageTitle(pageName, object);
		head = head.replace("[페이지 타이틀]", pageTitle);

		/* Meta Tag & Open Graph 작업 시작 */
		String siteTitle = "Dev_J BLOG";
		String siteSubject = "Dev_J의 BLOG";
		String siteKeywords = "JAVA, HTML, CSS, JavaScript, MySQL";
		String siteDescription = "Dev_J의 BLOG입니다.";
		String siteDomain = Container.appConfig.getSiteDomain();
		String siteMainUrl = "https://" + siteDomain;
		String currentDate = Util.getNowDateStr().replace(" ", "T");

		// 게시물 상세페이지마다 내용 나오게 하기
		if (object instanceof Article) {
			Article article = (Article) object;
			siteSubject = article.getTitle();
			siteDescription = article.getBody();
			siteDescription = siteDescription.replaceAll("[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]", "");
			// [^\uAC00-\uD7A3xfe0-9a-zA-Z\\s] => 모든 특수문자 제거
		}

		head = head.replace("[사이트 타이틀]", siteTitle);
		head = head.replace("[사이트 주제]", siteSubject);
		head = head.replace("[사이트 키워드]", siteKeywords);
		head = head.replace("[사이트 설명]", siteDescription);
		head = head.replace("[사이트 도메인]", siteDomain);
		head = head.replace("[사이트 URL]", siteMainUrl);
		head = head.replace("[날짜]", currentDate);

		/* Meta Tag & Open Graph 작업 끝 */
		
		return head;
	}

	// 입력받은 pageName에 맞는 페이지의 타이틀을 리턴
	private String getPageTitle(String pageName, Object object) {
		StringBuilder pageTitle = new StringBuilder();

		String forPrintPageName = pageName;

		if (forPrintPageName.equals("index")) {
			forPrintPageName = "home";
		}

		if (forPrintPageName.equals("search")) {
			forPrintPageName = "search";
		}

		forPrintPageName = forPrintPageName.toUpperCase(); // 대상 문자열을 모두 대문자로 변환
		forPrintPageName = forPrintPageName.replace("_", " "); // pageName에 있는 _를 공백으로 변환

		pageTitle.append("Dev_J BLOG | ");
		pageTitle.append(forPrintPageName);

		// object가 Article이라는 객체로 형변환이 가능한지를 판단
		if (object instanceof Article) {
			Article article = (Article) object;

			pageTitle.insert(0, article.getTitle() + " | ");
			// 형변환이 가능하면 0번 위치에 article.getTitle() + "|" 삽입
		}

		return pageTitle.toString();
	}

	// pageName에 따라 타이틀바 아이콘 가져오기
	private String getTitleBarContentByPageName(String pageName) {
		if (pageName.equals("index")) {
			return "";
		} else if (pageName.equals("article_detail")) {
			return "<i class=\"fas fa-file-invoice\"></i> <span>ARTICLE</span>";
		} else if (pageName.startsWith("article_list_notice")) {
			return "<i class=\"fas fa-bullhorn\"></i> <span>NOTICE BOARD</span>";
		} else if (pageName.startsWith("article_list_free")) {
			return "<i class=\"fas fa-users\"></i> <span>FREE BOARD</span>";
		} else if (pageName.startsWith("article_list_java")) {
			return "<i class=\"fab fa-java\"></i> <span>JAVA BOARD</span>";
		} else if (pageName.startsWith("article_list_html")) {
			return "<i class=\"fab fa-html5\"></i> <span>HTML & CSS & JS BOARD</span>";
		} else if (pageName.startsWith("article_list_mysql")) {
			return "<i class=\"fas fa-database\"></i> <span>MySQL BOARD</span>";
		} else if (pageName.startsWith("article_list_etc")) {
			return "<i class=\"fas fa-mouse-pointer\"></i> <span>ETC BOARD</span>";
		} else if (pageName.startsWith("project_list_p_blog")) {
			return "<i class=\"fab fa-blogger\"></i> <span>P.BLOG PROJECT</span>";
		} else if (pageName.startsWith("project_list_p_jsp")) {
			return "<i class=\"fas fa-users\"></i> <span>P.JSP PROJECT</span>";
		} else if (pageName.startsWith("project_list_p_lamp")) {
			return "<i class=\"fab fa-vuejs\"></i> <span>P.Lamplight PROJECT</span>";
		} else if (pageName.startsWith("project_list_p_what")) {
			return "<i class=\"fas fa-file-word\"></i> <span>P.WhatIsMyName PROJECT</span>";
		} else if (pageName.startsWith("search")) {
			return "<i class=\"fas fa-search\"></i> <span>SEARCH</span>";
		} else if (pageName.startsWith("statistics")) {
			return "<i class=\"fas fa-chart-pie\"></i> <span>STATISTICS</span>";
		} else if (pageName.startsWith("about")) {
			return "";
		}
		return "";
	}

}