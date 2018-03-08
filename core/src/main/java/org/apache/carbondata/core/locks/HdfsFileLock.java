/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.carbondata.core.locks;

import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.carbondata.common.logging.LogService;
import org.apache.carbondata.common.logging.LogServiceFactory;
import org.apache.carbondata.core.constants.CarbonCommonConstants;
import org.apache.carbondata.core.datastore.impl.FileFactory;
import org.apache.carbondata.core.metadata.AbsoluteTableIdentifier;

/**
 * This class is used to handle the HDFS File locking.
 * This is achieved using the concept of acquiring the data out stream using Append option.
 */
public class HdfsFileLock extends AbstractCarbonLock {

  private static final LogService LOGGER =
             LogServiceFactory.getLogService(HdfsFileLock.class.getName());
  /**
   * location hdfs file location
   */
  private String location;

  private String locationPath;

  private DataOutputStream dataOutputStream;

  /**
   * @param lockFileLocation
   * @param lockFile
   */
  public HdfsFileLock(String lockFileLocation, String lockFile) {
    this.locationPath = lockFileLocation
        + CarbonCommonConstants.FILE_SEPARATOR + LockUsage.LOCK_DIR;
    this.location = this.locationPath
        + CarbonCommonConstants.FILE_SEPARATOR + lockFile;
    LOGGER.info("HDFS lock path:" + this.location);
    initRetry();
  }

  /**
   * @param lockFilePath
   */
  public HdfsFileLock(String lockFilePath) {
    this.location = lockFilePath;
    initRetry();
  }

  /**
   * @param absoluteTableIdentifier
   * @param lockFile
   */
  public HdfsFileLock(AbsoluteTableIdentifier absoluteTableIdentifier, String lockFile) {
    this(absoluteTableIdentifier.getTablePath(), lockFile);
  }

  /* (non-Javadoc)
   * @see org.apache.carbondata.core.locks.ICarbonLock#lock()
   */
  @Override public boolean lock() {
    try {
      if (null != this.locationPath &&
          !FileFactory.isFileExist(locationPath, FileFactory.getFileType(locationPath))) {
        FileFactory.mkdirs(locationPath, FileFactory.getFileType(locationPath));
      }
      if (!FileFactory.isFileExist(location, FileFactory.getFileType(location))) {
        FileFactory.createNewLockFile(location, FileFactory.getFileType(location));
      }
      dataOutputStream =
          FileFactory.getDataOutputStreamUsingAppend(location, FileFactory.getFileType(location));

      return true;

    } catch (IOException e) {
      LOGGER.info(e.getMessage());
      return false;
    }
  }

  /* (non-Javadoc)
   * @see org.apache.carbondata.core.locks.ICarbonLock#unlock()
   */
  @Override
  public boolean unlock() {
    boolean status = false;
    if (null != dataOutputStream) {
      try {
        dataOutputStream.close();
        status = true;
      } catch (IOException e) {
        status = false;
      }
    }
    return status;
  }

}
