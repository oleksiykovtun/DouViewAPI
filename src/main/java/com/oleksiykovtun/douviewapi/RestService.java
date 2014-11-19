package com.oleksiykovtun.douviewapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.googlecode.objectify.ObjectifyService;
import com.oleksiykovtun.douviewapi.entities.*;
import java.util.logging.Logger;

/**
 * REST service server side.
 */
@Path("/")
public class RestService {

    private static final String CHARSET_UTF_8 = ";charset=utf-8";
    private static final String JSON = MediaType.APPLICATION_JSON + CHARSET_UTF_8;

    static {
        ObjectifyService.register(Author.class);
        ObjectifyService.register(Comment.class);
        ObjectifyService.register(Topic.class);
    }

    @GET
    @Path("/cache/{n}-recent-topics")
    @Produces(JSON)
    public List<Topic> getTopicsFromDb(@PathParam("n") int topicsCount) {
        List<Topic> topicList = null;
        Logger.getLogger("").info("Request processing started...");
        try {
            Logger.getLogger("").info("Loading from DB...");
            topicList = new ArrayList<Topic>(ObjectifyService.ofy().load().type(Topic.class)
                    .order("-timestamp").limit(topicsCount).list());
        } catch (Throwable e) {
            Logger.getLogger("").log(Level.SEVERE, "Request failed!", e);
        }
        Logger.getLogger("").info("Done.");
        return topicList;
    }

    @GET
    @Path("/direct/{n}-topic-heads")
    @Produces(JSON)
    public List<TopicHead> getTopicHeadsDirectly(@PathParam("n") int topicsCount) {
        return getTopicHeadsDirectly(DouFetcher.ALL_FORUMS, topicsCount);
    }

    @GET
    @Path("/direct/{subforum}/{n}-topic-heads")
    @Produces(JSON)
    public List<TopicHead> getTopicHeadsDirectly(@PathParam("subforum") String subForum, @PathParam("n") int topicsCount) {
        if (Arrays.asList(DouFetcher.SUBFORUMS).contains(subForum)) {
            List<TopicHead> topicHeadList = null;
            long processingTimeMillis = System.currentTimeMillis();
            Logger.getLogger("").info("Request processing started...");
            try {
                topicHeadList = DouFetcher.getTopicHeadList(topicsCount, subForum);
            } catch (Throwable e) {
                Logger.getLogger("").log(Level.SEVERE, "Request failed!", e);
            }
            processingTimeMillis = System.currentTimeMillis() - processingTimeMillis;
            Logger.getLogger("").info("Done in " + processingTimeMillis / 1000 + " s.");
            return topicHeadList;
        }
        return null;
    }

    @GET
    @Path("/direct/{n}-topics")
    @Produces(JSON)
    public List<Topic> getTopicsDirectly(@PathParam("n") int topicsCount) {
        return getTopicsDirectly(DouFetcher.ALL_FORUMS, topicsCount);
    }

    @GET
    @Path("/direct/{subforum}/{n}-topics")
    @Produces(JSON)
    public List<Topic> getTopicsDirectly(@PathParam("subforum") String subForum, @PathParam("n") int topicsCount) {
        if (Arrays.asList(DouFetcher.SUBFORUMS).contains(subForum)) {
            List<Topic> topicList = null;
            long processingTimeMillis = System.currentTimeMillis();
            Logger.getLogger("").info("Request processing started...");
            try {
                topicList = DouFetcher.getTopicList(topicsCount, subForum);
                Logger.getLogger("").info("Saving to DB...");
                ObjectifyService.ofy().save().entities(topicList).now();
            } catch (Throwable e) {
                Logger.getLogger("").log(Level.SEVERE, "Request failed!", e);
            }
            processingTimeMillis = System.currentTimeMillis() - processingTimeMillis;
            Logger.getLogger("").info("Done in " + processingTimeMillis / 1000 + " s.");
            return topicList;
        }
        return null;
    }

    @DELETE
    @Path("/cache/clear")
    public void deleteData() {
        Logger.getLogger("").info("Request processing started...");
        try {
            Logger.getLogger("").info("Clearing DB...");
            ObjectifyService.ofy().delete().keys(ObjectifyService.ofy().load().type(Topic.class).keys());
        } catch (Throwable e) {
            Logger.getLogger("").log(Level.SEVERE, "Request failed!", e);
        }
        Logger.getLogger("").info("Done.");
    }

}
