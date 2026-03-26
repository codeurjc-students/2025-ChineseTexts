package com.chinesereads.backend.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

@Service
public class JiebaService {         //Segmenta el texto en palabras

    public List<String> segment(String text){
        JiebaSegmenter segmenter = new JiebaSegmenter();
        List<SegToken> tokens = segmenter.process(text, JiebaSegmenter.SegMode.SEARCH);
        return tokens.stream().map(token -> token.word).collect(Collectors.toList());
    }

}