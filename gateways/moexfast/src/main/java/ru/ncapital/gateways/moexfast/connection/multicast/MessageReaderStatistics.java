package ru.ncapital.gateways.moexfast.connection.multicast;

import org.slf4j.Logger;
import ru.ncapital.gateways.moexfast.Utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by egore on 23/2/2017.
 */
public class MessageReaderStatistics {

    private static final int NUMBER_OF_LISTS = 100;

    private class StatisticsItem {
        // ALL TIMES IN TODAY MICROS (micros since midnight)

        private int seqNum;

        private long entrTime;

        private long sendTime;

        private long recvTime;

        private long decdTime;

        private StatisticsItem(int seqNum, long entrTime, long sendTime, long recvTime, long decdTime) {
            this.seqNum = seqNum;
            this.entrTime = entrTime;
            this.sendTime = sendTime;
            this.recvTime = recvTime;
            this.decdTime = decdTime;
        }
    }

    private List<List<StatisticsItem>> allItems = new ArrayList<>();

    private List<StatisticsItem> currentItems;

    private int currentItemsPos = 0;

    private long total = 0;

    private BufferedWriter writer;

    private boolean active = false;

    private Executor executor = Executors.newSingleThreadExecutor();

    private Logger logger;

    boolean isActive() {
        return active;
    }

    MessageReaderStatistics(Logger logger) {
        this.logger = logger;
    }

    public void initStatistics() {
        active = true;
        for (int i = 0; i < NUMBER_OF_LISTS; ++i)
            allItems.add(new ArrayList<StatisticsItem>());
        currentItems = allItems.get(0);
    }

    public void initStatisticsWritingToFile(String filename) {
        if (!active)
            return;

        try {
            writer = new BufferedWriter(new FileWriter(filename, false));
        } catch (IOException e) {
            System.err.println("FAILED TO OPEN FILE " + e.toString());
            writer = null;
        }
    }

    synchronized void addItem(int seqNum, long entrTime, long sendTime, long recvTime, long decdTime) {
        if (!active)
            return;

        currentItems.add(new StatisticsItem(seqNum, entrTime, sendTime, recvTime, decdTime));
    }

    public void dump() {
        if (active) {
            int pos = currentItemsPos;

            logger.info(pos + " " + Utils.currentTimeInTodayMicros());

            List<StatisticsItem> lastItems = initNextAvaiableItems();

            calculateAndWriteToFile(pos, lastItems);
        }
    }

    private synchronized List<StatisticsItem> initNextAvaiableItems() {
        List<StatisticsItem> itemsToDump = currentItems;

        if (currentItemsPos + 1 == NUMBER_OF_LISTS)
            currentItemsPos = 0;

        currentItems = allItems.get(++currentItemsPos);
        currentItems.clear();

        return itemsToDump;
    }

    private void calculateAndWriteToFile(int pos, List<StatisticsItem> items) {
        if (items.isEmpty())
            return;

        class CalculateAndWriteTask implements Runnable {
            private int pos;

            private List<StatisticsItem> items;

            private CalculateAndWriteTask(int pos, List<StatisticsItem> items) {
                this.pos = pos;
                this.items = items;
            }

            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                List<Long> latenciesEntrToRecv = new ArrayList<>();
                List<Long> latenciesEntrToSend = new ArrayList<>();

                for (StatisticsItem item : items) {
                    latenciesEntrToRecv.add(item.recvTime - item.entrTime);
                    latenciesEntrToSend.add(item.sendTime - item.entrTime);
                }

                Collections.sort(latenciesEntrToRecv);
                Collections.sort(latenciesEntrToSend);

                total += items.size();
                sb.append("[Total: ").append(total).append("]");
                sb.append("[Last: ").append(items.size()).append("]");
                sb.append("[MinL: ").append(String.format("%d", latenciesEntrToRecv.get(0))).append("");
                sb.append("|").append(String.format("%d", latenciesEntrToSend.get(0))).append("]");
                sb.append("[MedL: ").append(String.format("%d", latenciesEntrToRecv.get(latenciesEntrToRecv.size() / 2))).append("");
                sb.append("|").append(String.format("%d", latenciesEntrToSend.get(latenciesEntrToSend.size() / 2))).append("]");
                sb.append("[MaxL: ").append(String.format("%d", latenciesEntrToRecv.get(latenciesEntrToRecv.size() - 1))).append("");
                sb.append("|").append(String.format("%d", latenciesEntrToSend.get(latenciesEntrToSend.size() - 1))).append("]");

                logger.info(pos + " " + Utils.currentTimeInTodayMicros() + " " + sb.toString());

                if (writer == null)
                    return;

                try {
                    writer.write(sb.toString());
                    writer.newLine();

                    for (StatisticsItem item : items) {
                        writer.write(new StringBuilder()
                                .append(item.seqNum).append(";")
                                .append(item.entrTime).append(";")
                                .append(item.sendTime).append(";")
                                .append(item.recvTime).append(";")
                                .append(item.decdTime).append(";")
                                .append(item.recvTime - item.entrTime).append(";")
                                .append(item.sendTime - item.entrTime).append(";").toString());

                        writer.newLine();
                    }
                    writer.flush();
                } catch (IOException e) {
                    System.err.println("FAILED TO WRITE TO FILE " + e.toString());
                }
            }
        }

        executor.execute(new CalculateAndWriteTask(pos, items));
    }
}
