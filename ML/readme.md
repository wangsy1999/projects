# 1. Contribution

## 1.1 Data Collection

We collected weather data from all weather stations in NYC, spanning from December 15, 2021, to December 15, 2022, sourced from NOAA (National Centers for Environmental Information). Additionally, we obtained hourly bike-sharing usage data for New York City from 2016 via NYC Open Data, and comprehensive air pollution data for 2021 and 2022 from the EPA (Environmental Protection Agency).

- [NOAA Weather Data](https://www.ncei.noaa.gov/cdo-web/)
- [NYC Bike-Sharing Data](https://data.cityofnewyork.us/Transportation/Bicycle-Counts/uczf-rk3c)
- [EPA Air Quality Data](https://www.epa.gov/outdoor-air-quality-data/download-daily-data)

## 1.2 Data Cleaning

We cleaned and merged these datasets to prepare a comprehensive training dataset that included the number of bike-sharing users, air pollution levels, and weather conditions.

The datasets comprised:
1. **Air Pollution Data (2021-2022)**: Valid fields included PM2.5 and AQI, with dates formatted as MM/DD/YYYY.
2. **Bike Rentals Data**: This dataset provided the number of bike rentals recorded every half hour across various locations. We consolidated these to compute the daily number of rentals.
3. **Weather Data**: This included daily wind speed, temperature, and weather conditions. The data from different stations were averaged to represent overall city-wide conditions for that day. Missing values were handled by removing stations that didn't contribute data on a given day, and, where necessary, fields were omitted entirely.

Finally, we created a transform function to unify the date formats across all datasets, and merged them into a single dataset, `test.csv`, for model training.

---

# 2. Model Training

We selected a **random forest regression model**, focusing on two key parameters: the depth of the decision trees and the number of trees. Additionally, we considered the impact of data from the past few days on predicting the target value for the next day. We tuned the model by adjusting three parameters: maximum tree depth, number of decision trees, and the number of past days included in the prediction.

For training, we employed **k-fold cross-validation** to determine the best train-test split. We also iteratively created datasets merging `k` days' worth of data from the original dataset, testing tree depths between 10 and 20, and the number of decision trees between 50 and 150. We evaluated the models using **R²** and **MSE** metrics.

- **Model Performance**: 
    - In our experiments with MaxDepth=12 and `day=8`, the MSE displayed high variability across some groups, especially in the first and eighth groups. However, continuing the tuning process, we identified the optimal number of decision trees as 68.
    - For R², the performance also varied, with some groups even showing negative values, indicating poor predictions. However, the highest R² value was achieved with 62 decision trees.

- **Final Results**: 
    - The best prediction was achieved with 62 decision trees and a maximum tree depth of 12, resulting in an **R² score of 0.831**. This demonstrates that the model can reliably predict the number of trips with the given dataset and parameters.
