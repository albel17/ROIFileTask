import java.io.*;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ROIFileTask {
    volatile static Boolean stopped = false;
    static WatchService watcher;

    public static void main(String[] args) {
        Properties prop = new Properties();
        final File folder;
        final File avgFolder;

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        final Thread stopThread = new Thread(new Runnable() {
            public void run() {
                while(!stopped){
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    String input = "";
                    try {
                        input = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(input.toUpperCase().equals("STOP")){
                        stopped = true;
                        try {
                            watcher.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        stopThread.start();

        //WatchService watcher;
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream resourceStream = loader.getResourceAsStream("config.properties");
            prop.load(resourceStream);

            folder = new File(prop.getProperty("inputFolder"));
            avgFolder = new File(prop.getProperty("outputFolder"));

            watcher = FileSystems.getDefault().newWatchService();
            Path dir = folder.toPath();
            WatchKey key = dir.register(watcher,
                    ENTRY_CREATE,
                    ENTRY_DELETE,
                    ENTRY_MODIFY);

            if (!avgFolder.exists())
                avgFolder.mkdir();
            if (!folder.exists())
                folder.mkdir();


            while (!stopped) {
                key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == ENTRY_CREATE) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        final Path child = dir.resolve(filename);

                        executorService.execute(new Runnable() {
                            public void run() {
                                readFile(new File(String.valueOf(child)));
                            }
                        });
                    }

                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
            }
        }
        catch (ClosedWatchServiceException e){
            System.out.println("Application was stopped.");
        }
        catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    private static void readFile(File file) {
        BufferedReader br = null;
        boolean noErrors = false;
        List<ViewItem> list = new LinkedList<ViewItem>();
        try {
            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();

            while (line != null) {
                list.add(new ViewItem(line));
                line = br.readLine();
            }

            noErrors = processFile(list, file.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                    if (noErrors) {
                        Files.delete(file.toPath());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean processFile(List<ViewItem> list, String fileName) {
        long minTime = list.get(0).getStartTime();
        List<ViewItem> list2 = new LinkedList<ViewItem>();

        for (ViewItem item : list) {
            if (item.getStartTime() < minTime)
                minTime = item.getStartTime();
            addToList(list2, item);
        }

        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        calendar.setTimeInMillis(minTime);

        try {
            PrintWriter out = new PrintWriter(new File("C:\\folder\\avg\\avg_" + fileName));
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            boolean found = true;
            Calendar tomorrow = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
            tomorrow.setTimeInMillis(minTime);
            tomorrow.set(Calendar.HOUR_OF_DAY, 0);
            tomorrow.set(Calendar.MINUTE, 0);
            tomorrow.set(Calendar.SECOND, 0);
            tomorrow.set(Calendar.MILLISECOND, 0);
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            while (found) {
                List<ViewItem> tomorrowList = new LinkedList<ViewItem>();
                List<ViewItem> todayList = new LinkedList<ViewItem>();
                found = false;
                out.println(sdf.format(calendar.getTime()).toUpperCase());
                out.println();
                for (ViewItem item : list2) {
                    if (item.getStartTime() < tomorrow.getTimeInMillis())
                        makeNoRepeatList(todayList, list2, item, tomorrow);
                    else {
                        found = true;
                        tomorrowList.add(item);
                    }
                }
                sortViewItemList(todayList);
                for (ViewItem item : todayList) {
                    out.println(item.getUserID() + "," + item.getLink() + "," + item.getAllTime() / 1000);
                }
                tomorrow.add(Calendar.DAY_OF_MONTH, 1);
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                list2 = tomorrowList;
                out.println();
            }
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void addToList(List<ViewItem> list, ViewItem item) {
        Calendar calendar1 = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        Calendar calendar2 = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        calendar1.setTimeInMillis(item.getStartTime());
        calendar2.setTimeInMillis(item.getStartTime() + item.getAllTime());
        if (calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)) {
            list.add(item);
        } else {
            calendar1.set(Calendar.HOUR_OF_DAY, 0);
            calendar1.set(Calendar.MINUTE, 0);
            calendar1.set(Calendar.SECOND, 0);
            calendar1.set(Calendar.MILLISECOND, 0);
            calendar1.add(Calendar.DAY_OF_MONTH, 1);
            list.add(new ViewItem(item.getStartTime(), item.getUserID(), item.getLink(), calendar1.getTimeInMillis() - item.getStartTime()));
            item.setAllTime(item.getAllTime() - (calendar1.getTimeInMillis() - item.getStartTime()));
            item.setStartTime(calendar1.getTimeInMillis());
            addToList(list, item);
        }
    }

    private static void makeNoRepeatList(List<ViewItem> todayList, List<ViewItem> list, ViewItem item, Calendar tomorrow) {
        if (!hasElement(todayList, item)) {
            int i = 1;
            long sum = item.getAllTime();
            for (ViewItem listItem : list) {
                if (listItem.getStartTime() < tomorrow.getTimeInMillis() && !listItem.equals(item) && listItem.getUserID().equals(item.getUserID()) && listItem.getLink().equals(item.getLink())) {
                    i++;
                    sum += listItem.getAllTime();
                }
            }
            item.setAllTime(sum / i);
            todayList.add(item);
        }
    }

    private static boolean hasElement(List<ViewItem> list, ViewItem item) {
        boolean found = false;
        for (ViewItem listItem : list) {
            if (listItem.getUserID().equals(item.getUserID()) && listItem.getLink().equals(item.getLink())) {
                found = true;
                break;
            }
        }
        return found;
    }

    private static void sortViewItemList(List<ViewItem> list) {
        Collections.sort(list, new Comparator<ViewItem>() {
            public int compare(ViewItem o1, ViewItem o2) {
                if (o1.getUserID().equals(o2.getUserID()))
                    return 0;
                try {
                    if (Integer.parseInt(o1.getUserID().substring(4)) < Integer.parseInt(o2.getUserID().substring(4)))
                        return -1;
                    else
                        return 1;
                }
                catch (NumberFormatException e){
                    if(o1.getUserID().compareTo(o2.getUserID())<0)
                        return -1;
                    else
                        return  1;
                }
            }
        });
    }
}
