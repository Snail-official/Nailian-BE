package art.snail.naillian.backend.domain.nail.util;

import art.snail.naillian.backend.errors.ReportableError;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class NailAttributeSampler {

    /**
     * weightMatrix: 2차원 배열. 각 행은 해당 속성(색상, 쉐입, 패턴)의 누적 가중치를 나타냄.
     *   - weightMatrix[0]: 색상 (예: 길이 8)
     *   - weightMatrix[1]: 쉐입 (예: 길이 5)
     *   - weightMatrix[2]: 패턴 (예: 길이 4)
     * numSamples: 최종 추천할 (색상, 쉐입, 패턴) 조합의 개수 (기본 15)
     * temperature: softmax에 적용할 온도 파라미터
     *
     * @param weightMatrix 2차원 가중치 배열
     * @param numSamples   샘플링할 조합 수
     * @param temperature  온도 파라미터
     * @return int[][] 배열, 각 행은 [색상 index, 쉐입 index, 패턴 index] 조합
     */

    public static int[][] sampleNailAttributesFromMatrix(double[][] weightMatrix, int numSamples, double temperature){
        if(weightMatrix == null || weightMatrix.length != 3){
            throw new ReportableError(HttpStatus.BAD_REQUEST, "weightMatrix는 3행(색상, 쉐입, 패턴)을 가져아 합니다.");
        }

        double[] weightColor = weightMatrix[0];
        double[] weightShape = weightMatrix[1];
        double[] weightPattern = weightMatrix[2];

        int numColors = weightColor.length;
        int numShapes = weightShape.length;
        int numPatterns = weightPattern.length;

        // 카테시안 활용해서 모든 가능한 조합 생성
        List<int[]> combos = new ArrayList<>(Math.min(numColors * numShapes * numPatterns, 1000)); // 최대 1000개까지만 조합
        for (int c = 0; c < numColors; c++) {
            for (int s = 0; s < numShapes; s++) {
                for (int p = 0; p < numPatterns; p++) {
                    if (combos.size() >= 1000) break; // 메모리 초과 방지
                    combos.add(new int[]{c, s, p});
                }
            }
        }

        int totalCombos = combos.size();

        // 각 조합의 점수 계산: 색상 + 쉐입 + 패턴 가중치 + 엡실론(0 방지)
        double[] scores = new double[totalCombos];
        double eps = 1e-5;
        for(int i = 0; i < totalCombos; i++){
            int[] combo = combos.get(i);
            int c = combo[0], s = combo[1], p =combo[2];
            scores[i] = weightColor[c] + weightShape[s] + weightPattern[p] + eps;
        }

        // 소포트맥스 적용
        double[] scaledScores = new double[totalCombos];
        for (int i =0; i<totalCombos; i++){
            scaledScores[i] = scores[i] / temperature;
            }

        // 수치 안정을 위해 최댓값은 빼줌
        double maxScore = Double.NEGATIVE_INFINITY;
        for (int i =0; i<totalCombos; i++){
            if(scaledScores[i] > maxScore){
                maxScore = scaledScores[i];
            }
        }

        double[] expScores = new double[totalCombos];
        double sumExp = 0.0;
        for (int i =0; i <totalCombos; i++){
            expScores[i] = Math.exp(scaledScores[i] - maxScore);
            sumExp += expScores[i];
        }

        double[] probabilites = new double[totalCombos];
        for(int i =0; i<totalCombos; i++){
            probabilites[i] = expScores[i] / sumExp;
        }

        // 확률에 따라 중복 없이 numSamples개 샘플링 (weighted sampling without replacement)
        List<Integer> candidateIndices = new ArrayList<>();
        for (int i = 0; i < totalCombos; i++) {
            candidateIndices.add(i);
        }
        List<Integer> chosenIndices = new ArrayList<>();
        Random random = ThreadLocalRandom.current();

        for (int sample = 0; sample < numSamples; sample++) {
            double totalProb = 0.0;
            for (int index : candidateIndices) {
                totalProb += probabilites[index];
            }
            double r = random.nextDouble() * totalProb;
            double cumulative = 0.0;
            int chosen = -1;
            for (int index : candidateIndices) {
                cumulative += probabilites[index];
                if (cumulative >= r) {
                    chosen = index;
                    break;
                }
            }
            if (chosen == -1 && !candidateIndices.isEmpty()) {
                chosen = candidateIndices.get(candidateIndices.size() - 1);
            }
            chosenIndices.add(chosen);
            candidateIndices.remove(Integer.valueOf(chosen));
        }

        // 선택된 인덱스에 해당하는 조합들을 결과 배열에 저장
        int[][] result = new int[numSamples][3];
        for (int i = 0; i < numSamples; i++) {
            result[i] = combos.get(chosenIndices.get(i));
        }
        return result;
    }
}
