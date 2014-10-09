package com.oleksiykovtun.douview.server;

import com.oleksiykovtun.douview.server.webpagedataextractor.Node;
import com.oleksiykovtun.douview.server.webpagedataextractor.NodeList;
import com.oleksiykovtun.douview.server.webpagedataextractor.UserAgent;
import com.oleksiykovtun.douview.server.webpagedataextractor.WebpageDataExtractor;
import com.oleksiykovtun.douview.shared.entities.Author;
import com.oleksiykovtun.douview.shared.entities.Comment;
import com.oleksiykovtun.douview.shared.entities.Topic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Forum topics fetcher from DOU.ua
 */
public class DouFetcher {

    private static final String topicsListPageUrlStringPrefix = "http://dou.ua/forums/page/";


    public static List<String> getTopicUrlStringList(String forumUrlString, int topicsCount) throws Exception {
        List<String> topicUrlStringList = new ArrayList<String>();
        int topicsPageCount = 0;
        while (topicUrlStringList.size() < topicsCount) {
            ++topicsPageCount;
            Logger.getLogger("").info("Topics list page " + topicsPageCount + "...");
            List<String> pageTopicUrlStringList = getTopicUrlStringListFromPage(forumUrlString + topicsPageCount);
            while (pageTopicUrlStringList.size() > 0 && topicUrlStringList.size() < topicsCount) {
                topicUrlStringList.add(pageTopicUrlStringList.remove(0));
            }
        }
        Logger.getLogger("").info("Done.");
        return topicUrlStringList;
    }

    public static List<Topic> getTopicList(int topicsCount) throws Exception {
        List<String> topicUrlStringList = DouFetcher.getTopicUrlStringList(topicsListPageUrlStringPrefix, topicsCount);
        return getTopicList(topicUrlStringList);
    }

    public static List<Topic> getTopicList(List<String> topicUrlStringList) throws Exception {
        List<Topic> topicList = new ArrayList<Topic>();
        for (int i = 0; i < topicUrlStringList.size(); ++i) {
            Logger.getLogger("").info("Topic " + (i + 1) + " out of " + topicUrlStringList.size() + "...");
            topicList.add(getTopic(topicUrlStringList.get(i)));
        }
        Logger.getLogger("").info("Done.");
        return topicList;
    }

    private static List<String> getTopicUrlStringListFromPage(String pageUrlString) throws Exception {
        Node document = WebpageDataExtractor.loadDocument(pageUrlString, UserAgent.MOBILE);
        NodeList topicsNodeList = getTopicNodeList(document);
        List<String> pageTopicUrlStringList = new ArrayList<String>();
        for (int i = 0; i < topicsNodeList.getCount(); ++i) {
            Node node = topicsNodeList.get(i);
            pageTopicUrlStringList.add(getTopicLink(node));
        }
        return pageTopicUrlStringList;
    }

    public static Topic getTopic(String topicUrlString) throws Exception {
        Node document = WebpageDataExtractor.loadDocument(topicUrlString, UserAgent.MOBILE);
        return new Topic(
                getTopicName(document),
                new Author(getTopicAuthorName(document), getTopicAuthorUrl(document)),
                getTopicMessage(document), getTopicCreationTime(document),
                getTopicCommentList(document), getTopicDouViewsCount(document),
                topicUrlString
        );
    }

    private static List<Comment> getTopicCommentList(Node document) throws Exception {
        NodeList commentNodeList = getCommentNodeList(document);
        List<Comment> commentList = new ArrayList<Comment>();
        for (int i = 0; i < commentNodeList.getCount(); ++i) {
            Node node = commentNodeList.get(i).getFirstChild("div").getFirstChild("div");
            commentList.add(getComment(node));
        }
        return commentList;
    }

    private static Comment getComment(Node node) {
        return new Comment(
                new Author(getCommentAuthorName(node), getCommentAuthorUrl(node)),
                getCommentToAuthorName(node), getCommentLevel(node),
                getCommentBody(node), getCommentLikesCount(node),
                getCommentCreationTime(node), getCommentLink(node)
        );
    }


    private static NodeList getTopicNodeList(Node document) throws Exception {
        return document
                .evaluateXPathList("//article");
    }

    private static String getTopicMessage(Node document) throws Exception {
        return document
                .evaluateXPath("//article")
                .evaluateXPath("//p").getTextContent();
    }

    private static String getTopicName(Node document) throws Exception {
        return document
                .evaluateXPath("//article").getFirstChild("h1").getTextContent();
    }

    private static String getTopicCreationTime(Node document) throws Exception {
        return document
                .evaluateXPath("//span[starts-with(@class, 'date')]").getTextContent();
    }

    private static int getTopicDouViewsCount(Node document) throws Exception {
        if (document
                .isXPathExisting("//span[starts-with(@class, 'pageviews')]")) {
            return document
                    .evaluateXPath("//span[starts-with(@class, 'pageviews')]").getFirstIntFromContent();
        }
        return 0;
    }

    private static String getTopicAuthorName(Node document) throws Exception {
        return document
                .evaluateXPath("//div[starts-with(@class, 'name')]")
                .getFirstChild("a").getTextContent();
    }

    private static String getTopicAuthorUrl(Node document) throws Exception {
        return document
                .evaluateXPath("//div[starts-with(@class, 'name')]")
                .getFirstChild("a").getAttributeValue(0);
    }

    private static String getTopicLink(Node topicNode) {
        return topicNode
                .getFirstChild("h2")
                .getFirstChild("a").getAttributeValue(0);
    }

    private static NodeList getCommentNodeList(Node document) throws Exception {
        return document
                .evaluateXPathList("//div[starts-with(@class, 'b-comment level-')]");
    }

    private static String getCommentAuthorName(Node commentNode) {
        return commentNode
                .getFirstChild("div")
                .getChild("a", 0).getTextContent();
    }

    private static String getCommentAuthorUrl(Node commentNode) {
        return commentNode
                .getFirstChild("div")
                .getChild("a", 0).getAttributeValue(1);
    }

    private static String getCommentCreationTime(Node commentNode) {
        return commentNode
                .getFirstChild("div")
                .getChild("a", 1).getTextContent();
    }

    private static String getCommentLink(Node commentNode) {
        return commentNode
                .getFirstChild("div")
                .getChild("a", 1).getAttributeValue(1);
    }

    private static String getCommentBody(Node commentNode) {
        return commentNode
                .getChild("div", 1).getTextContent();
    }

    private static String getCommentToAuthorName(Node commentNode) {
        if (commentNode
                .isChildExisting("div", 2)
                && commentNode
                .getChild("div", 2)
                .isChildExisting("a")) {
            return commentNode
                    .getChild("div", 2)
                    .getFirstChild("a").getTextContent();
        }
        return "";
    }

    private static int getCommentLikesCount(Node commentNode) {
        if (commentNode.isChildExisting("div", 3)
            && commentNode
                .getChild("div", 3).isChildExisting("span")) {
            return commentNode
                    .getChild("div", 3)
                    .getFirstChild("span").getFirstIntFromContent();
        }
        return 0;
    }

    private static int getCommentLevel(Node commentNode) {
        return commentNode
                .getParent()
                .getParent().getIntFromAttributeValue(0);
    }

}
