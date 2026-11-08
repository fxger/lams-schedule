package org.example.lamsserver.exception;

/**
 * 业务异常：由全局异常处理器转换为统一返回结构
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
