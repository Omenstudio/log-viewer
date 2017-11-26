package com.github.omenstudio.logviewer.search.listeners;

import com.github.omenstudio.logviewer.search.models.FoundFile;


/**
 * Слушатель для получения уведомлений о том,
 * что очередной файл успешно обработан "поисковиком",
 * и в нем найдены совпадения (одно или больше) по искомому запросу.
 *
 * @see FoundFile
 *
 * @author Василий
 */
public interface FileMatchFoundListener {

    /**
     * Срабатывает, когда файл полностью обработан поисковиком и в нем найдены совпадения.
     *
     * @see FoundFile
     *
     * @param resultFile Файл <code>FoundFile</code>, в котором сохранены результаты поиска
     */
    public void fileMatchFound(FoundFile resultFile);
}
