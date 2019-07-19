/*
 * Copyright 2014-2019 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.cluster;

import io.aeron.archive.Archive;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.status.SystemCounterDescriptor;
import org.agrona.CloseHelper;

import static org.agrona.SystemUtil.loadPropertiesFiles;

/**
 * Cluster Backup media driver which is an aggregate of a {@link MediaDriver}, {@link Archive},
 * and a {@link ClusterBackup}.
 */
public class ClusterBackupMediaDriver implements AutoCloseable
{
    private final MediaDriver driver;
    private final Archive archive;
    private final ClusterBackup clusterBackup;

    ClusterBackupMediaDriver(final MediaDriver driver, final Archive archive, final ClusterBackup clusterBackup)
    {
        this.driver = driver;
        this.archive = archive;
        this.clusterBackup = clusterBackup;
    }

    /**
     * Launch the cluster backup media driver aggregate and await a shutdown signal.
     *
     * @param args command line argument which is a list for properties files as URLs or filenames.
     */
    public static void main(final String[] args)
    {
        loadPropertiesFiles(args);

        try (ClusterBackupMediaDriver driver = launch())
        {
            driver.clusterBackup().context().shutdownSignalBarrier().await();

            System.out.println("Shutdown ClusterBackupMediaDriver...");
        }
    }

    /**
     * Launch a new {@link ClusterBackupMediaDriver} with default contexts.
     *
     * @return a new {@link ClusterBackupMediaDriver} with default contexts.
     */
    public static ClusterBackupMediaDriver launch()
    {
        return launch(new MediaDriver.Context(), new Archive.Context(), new ClusterBackup.Context());
    }

    /**
     * Launch a new {@link ClusterBackupMediaDriver} with provided contexts.
     *
     * @param driverCtx          for configuring the {@link MediaDriver}.
     * @param archiveCtx         for configuring the {@link Archive}.
     * @param clusterBackupCtx   for the configuration of the {@link ClusterBackup}.
     * @return a new {@link ClusterBackupMediaDriver} with the provided contexts.
     */
    public static ClusterBackupMediaDriver launch(
        final MediaDriver.Context driverCtx,
        final Archive.Context archiveCtx,
        final ClusterBackup.Context clusterBackupCtx)
    {
        final MediaDriver driver = MediaDriver.launch(driverCtx
            .spiesSimulateConnection(true));

        final Archive archive = Archive.launch(archiveCtx
            .mediaDriverAgentInvoker(driver.sharedAgentInvoker())
            .errorHandler(driverCtx.errorHandler())
            .errorCounter(driverCtx.systemCounters().get(SystemCounterDescriptor.ERRORS)));

        final ClusterBackup clusterBackup = ClusterBackup.launch(clusterBackupCtx);

        return new ClusterBackupMediaDriver(driver, archive, clusterBackup);
    }

    /**
     * Get the {@link MediaDriver} used in the aggregate.
     *
     * @return the {@link MediaDriver} used in the aggregate.
     */
    public MediaDriver mediaDriver()
    {
        return driver;
    }

    /**
     * Get the {@link Archive} used in the aggregate.
     *
     * @return the {@link Archive} used in the aggregate.
     */
    public Archive archive()
    {
        return archive;
    }

    /**
     * Get the {@link ClusterBackup} used in the aggregate.
     *
     * @return the {@link ClusterBackup} used in the aggregate.
     */
    public ClusterBackup clusterBackup()
    {
        return clusterBackup;
    }

    public void close()
    {
        CloseHelper.close(clusterBackup);
        CloseHelper.close(archive);
        CloseHelper.close(driver);
    }
}
