package com.javafxpert.neuralnetviz.controller;

import com.javafxpert.neuralnetviz.model.multilayernetwork.PredictionResponse;
import com.javafxpert.neuralnetviz.state.MultiLayerNetworkState;
import com.javafxpert.neuralnetviz.util.AppUtils;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * Created by jamesweaver on 7/18/16.
 */
@RestController
public class MultiLayerNetworkController {
  // The values parameter takes a comma separated list of numbers representing feature values
  @RequestMapping(value = "/prediction", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> renderClaims(@RequestParam(value = "values")
                                             String values) {

    PredictionResponse predictionResponse = null;

    double[] valuesArray = AppUtils.commaSeparatedNumbersToArray(values);
    int numValues = valuesArray.length;

    // Retrieve the model state
    MultiLayerNetwork network = MultiLayerNetworkState.getNeuralNetworkModel();

    int numInputColumns = network.getInput().columns();

    // Validate the number of values submitted into this service matches number of input values in the network
    if (numValues > 0 && numValues == numInputColumns) {

      predictionResponse = new PredictionResponse();

      // Make prediction
      // Input: 0.6236,-0.7822  Expected output: 1
      INDArray features = Nd4j.zeros(1, numValues);
      for (int valueIdx = 0; valueIdx < numValues; valueIdx++) {
        features.putScalar(new int[] { 0, valueIdx }, valuesArray[valueIdx]);
      }
      predictionResponse = predict(features);
    }

    return Optional.ofNullable(predictionResponse)
        .map(cr -> new ResponseEntity<>((Object)cr, HttpStatus.OK))
        .orElse(new ResponseEntity<>("Prediction unsuccessful", HttpStatus.INTERNAL_SERVER_ERROR));
  }

  /**
   * Modification of predict() method in dl4j library for the purpose of retrieving activation at prediction time
   * @param featuresMatrix
   * @return PredictionResponse
   */
  public PredictionResponse predict(INDArray featuresMatrix) {
    PredictionResponse retVal = new PredictionResponse();
    MultiLayerNetwork network = MultiLayerNetworkState.getNeuralNetworkModel();
    INDArray output = network.output(featuresMatrix, false);

    List<INDArray> layerActivationsList = network.feedForward(featuresMatrix);
    for (INDArray layerActivations : layerActivationsList) {
      for (int activationIdx = 0; activationIdx < layerActivations.length(); activationIdx++) {
        double activation = Math.round(layerActivations.getDouble(activationIdx) * 100) / 100d;
        retVal.getActivations().add(new Double(activation));
      }
    }

    int[] prediction = new int[featuresMatrix.size(0)];
    if (featuresMatrix.isRowVector()) prediction[0] = Nd4j.getBlasWrapper().iamax(output);
    else {
      for (int i = 0; i < prediction.length; i++)
        prediction[i] = Nd4j.getBlasWrapper().iamax(output.getRow(i));
    }
    retVal.setPrediction(prediction[0]);
    return retVal;
  }


}