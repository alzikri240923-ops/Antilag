package com.gtboost.app;

interface IRefreshRateService {
    String setRefreshRate(float hz);
    String releaseRefreshRate();
    void destroy();
}
