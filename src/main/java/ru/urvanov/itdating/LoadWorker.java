package ru.urvanov.itdating;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingWorker;

import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHUserSearchBuilder;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedSearchIterable;

import ru.urvanov.itdating.LoadPeopleFrame.CheckBlogChunk;
import ru.urvanov.itdating.LoadPeopleFrame.CountChunk;
import ru.urvanov.itdating.LoadPeopleFrame.DownloadAvatarChunk;
import ru.urvanov.itdating.LoadPeopleFrame.FilterChunk;
import ru.urvanov.itdating.LoadPeopleFrame.LoadChunk;
import ru.urvanov.itdating.LoadPeopleFrame.PrepareChunk;
import ru.urvanov.itdating.LoadPeopleFrame.SaveChunk;
import ru.urvanov.itdating.LoadPeopleFrame.SearchChunk;

public class LoadWorker extends SwingWorker<Void, LoadChunk> {

    private static final int COUNT_THREADS = 10;
    
    private ExecutorService countExecutorService = Executors.newFixedThreadPool(COUNT_THREADS);
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private ExecutorService joinExecutorService = Executors.newSingleThreadExecutor();


    private CanBeMoreQueue<PagedSearchIterable<GHUser>> inQueue = new CanBeMoreQueue<>();
    private CanBeMoreQueue<CountResult> countQueue = new CanBeMoreQueue<>();
    private CanBeMoreQueue<SearchResult> searchQueue = new CanBeMoreQueue<>();
    private CanBeMoreQueue<SearchResultItem> searchItemQueue = new CanBeMoreQueue<>();
    private CanBeMoreQueue<FilterResult> filterQueue = new CanBeMoreQueue<>();
    private CanBeMoreQueue<CheckBlogResult> checkBlogQueue = new CanBeMoreQueue<>();
    private CanBeMoreQueue<DownloadAvatarResult> downloadAvatarQueue = new CanBeMoreQueue<>();
    private CanBeMoreQueue<SaveResult> saveQueue = new CanBeMoreQueue<>();
    private CanBeMoreQueue<LoadChunk> chunksQueue = new CanBeMoreQueue<>();
    
    private List<Future<?>> countFutures = new ArrayList<>();
    private List<Future<?>> searchFutures = new ArrayList<>();
    private List<Future<?>> filterFutures = new ArrayList<>();
    private List<Future<?>> checkBlogFutures = new ArrayList<>();
    private List<Future<?>> downloadAvatarFutures = new ArrayList<>();
    private List<Future<?>> saveFutures = new ArrayList<>();

    private Future<?> readChunksFuture;
    
    private String gitHubAppInstallationToken;
    
    public LoadWorker(String gitHubAppInstallationToken) {
        super();
        this.gitHubAppInstallationToken = gitHubAppInstallationToken;
    }
    
    @Override
    protected Void doInBackground() throws Exception {

        GitHub github = new GitHubBuilder().withAppInstallationToken(
                gitHubAppInstallationToken).build();

        clearPeopleTable();
        List<PagedSearchIterable<GHUser>> listSearchIterables = createPagedSearchList(
                github);
        
        createCountWorkers(COUNT_THREADS);
        createSearchWorkers(1);
        createSearchToItemsWorker();
        createFilterWorkers(github, 1);
        createCheckBlogWorkers(100);
        createDownloadAvatarWorkers(100);
        createSaveWorkers(10);

        inQueue.addAll(listSearchIterables);
        inQueue.noMore();

        createReadChunksWorker();
        
        
        createJoinWorker(countFutures, countQueue, "Count", false).get();
        createJoinWorker(searchFutures, searchQueue, "Search", false).get();
        createJoinWorker(filterFutures, filterQueue, "Filter", false).get();
        createJoinWorker(checkBlogFutures, checkBlogQueue, "Check blog", false).get();
        createJoinWorker(downloadAvatarFutures, downloadAvatarQueue, "Download avatar", false).get();
        createJoinWorker(saveFutures, saveQueue, "Save", true).get();
        
        readChunksFuture.get();
        
        return null;
    }

    private Future<?> createJoinWorker(List<Future<?>> futures,
            CanBeMoreQueue<?> canBeMoreQueue, String joinWorkerName,
            boolean lastWorker) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                canBeMoreQueue.noMore();
                if (lastWorker) {
                    chunksQueue.noMore();
                }
                System.out.println(joinWorkerName + " finished.");
            }
        };
        return joinExecutorService.submit(runnable);
    }

    private void createCountWorkers(int count) {
        for (int n = 0; n < count; n++) {
            Runnable runnable = new InOutChunkProcessRunnable<>(inQueue, countQueue, chunksQueue, new CountFunction(), r -> new LoadChunk(new CountChunk(r.totalCount()), null, null, null, null, null, null));
            Future<?> future = countExecutorService.submit(runnable);
            countFutures.add(future);
        }
        
    }
    
    private void createSearchWorkers(int count) {
        for (int n = 0; n < count; n++) {
            Runnable runnable = new InOutChunkProcessRunnable<>(countQueue, searchQueue, chunksQueue, new SearchFunction(), r -> new LoadChunk(null, new PrepareChunk(), new SearchChunk(r.items().size()), null, null, null, null));
            Future<?> future = executorService.submit(runnable);
            searchFutures.add(future);
        }

    }
    
    private void createSearchToItemsWorker() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    SearchResult searchResult;
                    while (null != (searchResult = searchQueue.pollWithWait())) {
                         
                        searchItemQueue.addAll(searchResult.items());
                    }
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                System.out.println("Calling no more for searchToItems...");
                searchItemQueue.noMore();
                System.out.println("SearchToItems finished.");
            }
        };
        executorService.submit(runnable);
    }
    
    private void createFilterWorkers(GitHub github, int count) {
        for (int n = 0; n < count; n++) {
            Runnable runnable = new InOutChunkProcessRunnable<>(searchItemQueue, filterQueue, chunksQueue, new FilterFunction(github), r -> new LoadChunk(null, null, null, new FilterChunk(), null, null, null));
            Future<?> future = executorService.submit(runnable);
            filterFutures.add(future);
        }

    }
    
    private void createCheckBlogWorkers(int count) {
        for (int n = 0; n < count; n++) {
            Runnable runnable = new InOutChunkProcessRunnable<>(filterQueue, checkBlogQueue, chunksQueue, new CheckBlogFunction(), r -> new LoadChunk(null, null, null, null, new CheckBlogChunk(), null, null));
            Future<?> future = executorService.submit(runnable);
            checkBlogFutures.add(future);
        }
    }
    
    private void createDownloadAvatarWorkers(int count) {
        for (int n = 0; n < count; n ++ ) {
            Runnable runnable = new InOutChunkProcessRunnable<>(checkBlogQueue, downloadAvatarQueue, chunksQueue, new DownloadAvatarFunction(), r -> new LoadChunk(null, null, null, null, null, new DownloadAvatarChunk(), null));
            Future<?> future = executorService.submit(runnable);
            downloadAvatarFutures.add(future);
        }
    }

    private void createSaveWorkers(int count) {
        AtomicInteger idCounter = new AtomicInteger();
        for (int n = 0; n < count; n++) {
            Runnable runnable = new InOutChunkProcessRunnable<>(downloadAvatarQueue, saveQueue, chunksQueue, new SaveFunction(idCounter), r -> new LoadChunk(null, null, null, null, null, null, new SaveChunk()));
            Future<?> future = executorService.submit(runnable);
            saveFutures.add(future);
        }
    }

    
    private void clearPeopleTable() throws SQLException {
        try (Connection conn = App.getConnection();
                PreparedStatement deleteStatement = conn
                        .prepareStatement("delete from people")) {
            System.out.println("Erasing peoples table for new records...");
            deleteStatement.execute();
            conn.commit();

        }
    }
    
    private void createReadChunksWorker() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    LoadChunk loadChunk;
                    while (null != (loadChunk = chunksQueue.pollWithWait())) {
                        LoadWorker.this.publish(loadChunk);
                    }
                    System.out.println("Read chunks worker finished.");
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        };
        readChunksFuture = executorService.submit(runnable);
    }

    private List<PagedSearchIterable<GHUser>> createPagedSearchList(
            GitHub github) {
        List<PagedSearchIterable<GHUser>> listSearchIterables = new ArrayList<>();
        for (String womanName : WomanNames.WOMAN_NAMES) {
            for (String locationName : LocationNames.NAMES) {
                listSearchIterables.add(createPagedSearchIterable(github, womanName, locationName));
            }
        }
        return listSearchIterables;
    }

    private PagedSearchIterable<GHUser> createPagedSearchIterable(GitHub github,
            String womanName, String cityTownName) {
        GHUserSearchBuilder builder = github.searchUsers()
                // .q("@ in:email")
                .q("type:user");
        if (cityTownName != null) {
            builder.location(cityTownName);
        }
        if (womanName != null) {
            builder.q(womanName);
        }

        PagedSearchIterable<GHUser> list = builder.list();
        list.withPageSize(100);
        return list;
    }

}