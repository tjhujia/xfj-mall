package com.xfj.comment.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.xfj.comment.CommentException;
import com.xfj.comment.ICommentService;
import com.xfj.comment.constant.CommentRetCode;
import com.xfj.comment.convert.CommentConverter;
import com.xfj.comment.entitys.Comment;
import com.xfj.comment.entitys.CommentPicture;
import com.xfj.comment.mapper.CommentMapper;
import com.xfj.comment.mapper.CommentPictureMapper;
import com.xfj.comment.rs.*;
import com.xfj.comment.utils.ExceptionProcessorUtil;
import com.xfj.comment.utils.GlobalIdGeneratorUtil;
import com.xfj.comment.utils.SensitiveWordsUtil;
import com.xfj.comment.vo.*;
import com.xfj.order.OrderQueryService;
import com.xfj.order.dto.OrderDto;
import com.xfj.order.rs.OrderItemRS;
import com.xfj.order.vo.OrderItemVO;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

/**
 * @author heps
 * @date 2019/8/12 21:13
 * 商品评价服务实现
 */
@Service(group = "${dubbo-group.name}")
public class CommentServiceImpl implements ICommentService {

    private final CommentMapper commentMapper;

    private final CommentPictureMapper commentPictureMapper;

    private final CommentConverter commentConverter;

    @Reference(group = "${dubbo-group.name}")
    private OrderQueryService orderQueryService;

    private final GlobalIdGeneratorUtil globalIdGeneratorUtil;

    private static final String COMMENT_GLOBAL_ID_CACHE_KEY = "COMMENT_ID";

    private static final String COMMENT_PICTURE_GLOBAL_ID_CACHE_KEY = "COMMENT_PICTURE_ID";

    public CommentServiceImpl(CommentMapper commentMapper, CommentPictureMapper commentPictureMapper,
                              CommentConverter commentConverter, GlobalIdGeneratorUtil globalIdGeneratorUtil) {
        this.commentMapper = commentMapper;
        this.commentPictureMapper = commentPictureMapper;
        this.commentConverter = commentConverter;
        this.globalIdGeneratorUtil = globalIdGeneratorUtil;
    }

    @Override
    public AddCommentRS addComment(AddCommentVO request) {
        AddCommentRS response = new AddCommentRS();
        response.setCode(CommentRetCode.SUCCESS.getCode());
        response.setMsg(CommentRetCode.SUCCESS.getMessage());
        try {
            request.requestCheck();
            checkSensitiveWords(request.getContent());
            return doAddComment(request);
        } catch (Exception e) {
            ExceptionProcessorUtil.handleException(response, e);
        }
        return response;
    }

    @Override
    public CommentRS comment(CommentVO request) {
        CommentRS response = new CommentRS();
        try {
            request.requestCheck();

            String orderItemId = request.getOrderItemId();
            OrderItemVO orderItemRequest = new OrderItemVO();
            orderItemRequest.setOrderItemId(orderItemId);
            OrderItemRS orderItemResponse = orderQueryService.orderItem(orderItemRequest);

            String itemId = orderItemResponse.getItemId();
            String orderId = orderItemResponse.getOrderId();

            Example example = new Example(Comment.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("itemId", itemId);
            criteria.andEqualTo("orderId", orderId);
            criteria.andEqualTo("isDeleted", false);
            List<Comment> comments = commentMapper.selectByExample(example);
            if (CollectionUtils.isEmpty(comments)) {
                response.setCode(CommentRetCode.COMMENT_NOT_EXIST.getCode());
                response.setMsg(CommentRetCode.COMMENT_NOT_EXIST.getMessage());
            } else {
                response.setCode(CommentRetCode.SUCCESS.getCode());
                response.setMsg(CommentRetCode.SUCCESS.getMessage());
                response.setCommentDtoList(commentConverter.comment2Dto(comments));
            }
        } catch (Exception e) {
            ExceptionProcessorUtil.handleException(response, e);
        }
        return response;
    }

    @Override
    public CommentListRS commentList(CommentListVO request) {
        CommentListRS response = new CommentListRS();
        try {
            request.requestCheck();
            String itemId = request.getItemId();
            Integer type = request.getType();
            Example example = new Example(Comment.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("itemId", itemId);
            if (type != null) {
                criteria.andEqualTo("type", type.byteValue());
            }
            criteria.andEqualTo("isDeleted", false);
            PageHelper.startPage(request.getPage(), request.getSize());
            List<Comment> comments = commentMapper.selectByExample(example);
            if (CollectionUtils.isEmpty(comments)) {
                response.setCode(CommentRetCode.COMMENT_NOT_EXIST.getCode());
                response.setMsg(CommentRetCode.COMMENT_NOT_EXIST.getMessage());
            } else {
                response.setCode(CommentRetCode.SUCCESS.getCode());
                response.setMsg(CommentRetCode.SUCCESS.getMessage());
                response.setPage(request.getPage());
                response.setSize(request.getSize());
                PageInfo<Comment> commentPageInfo = new PageInfo<>();
                response.setTotal(commentPageInfo.getTotal());
                response.setCommentDtoList(commentConverter.comment2Dto(comments));
            }
        } catch (Exception e) {
            ExceptionProcessorUtil.handleException(response, e);
        }
        return response;
    }

    @Override
    public TotalCommentRS totalComment(TotalCommentVO request) {
        TotalCommentRS response = new TotalCommentRS();
        try {
            request.requestCheck();
            String itemId = request.getItemId();
            Integer type = request.getType();
            Example example = new Example(Comment.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("itemId", itemId);
            if (type != null) {
                criteria.andEqualTo("type", type.byteValue());
            }
            int count = commentMapper.selectCountByExample(example);
            response.setCode(CommentRetCode.SUCCESS.getCode());
            response.setMsg(CommentRetCode.SUCCESS.getMessage());
            response.setTotal(count);
        } catch (Exception e) {
            ExceptionProcessorUtil.handleException(response, e);
        }
        return response;
    }

    @Override
    public DeleteCommentRS deleteComment(DeleteCommentVO request) {
        DeleteCommentRS response = new DeleteCommentRS();
        try {
            request.requestCheck();
            String commentId = request.getCommentId();
            Comment comment = commentMapper.selectByPrimaryKey(commentId);
            if (comment == null || !comment.getIsDeleted()) {
                throw new CommentException(CommentRetCode.CURRENT_COMMENT_NOT_EXIST.getCode(), CommentRetCode.CURRENT_COMMENT_NOT_EXIST.getMessage());
            }
            comment.setDeletionUserId(request.getUserId());
            comment.setIsDeleted(true);
            comment.setDeletionTime(new Date());

            Example example = new Example(CommentPicture.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("commentId", commentId);
            commentPictureMapper.deleteByExample(example);

            response.setCode(CommentRetCode.SUCCESS.getCode());
            response.setMsg(CommentRetCode.SUCCESS.getMessage());
        } catch (Exception e) {
            ExceptionProcessorUtil.handleException(response, e);
        }
        return response;
    }

    @Override
    public TopCommentRS topComment(TopCommentVO request) {
        TopCommentRS response = new TopCommentRS();
        try {
            request.requestCheck();

            String commentId = request.getCommentId();
            Comment comment = commentMapper.selectByPrimaryKey(commentId);
            if (comment == null || !comment.getIsDeleted()) {
                throw new CommentException(CommentRetCode.CURRENT_COMMENT_NOT_EXIST.getCode(), CommentRetCode.CURRENT_COMMENT_NOT_EXIST.getMessage());
            }
            comment.setIsTop(true);
            commentMapper.updateByPrimaryKey(comment);
            response.setCode(CommentRetCode.SUCCESS.getCode());
            response.setMsg(CommentRetCode.SUCCESS.getMessage());
        } catch (Exception e) {
            ExceptionProcessorUtil.handleException(response, e);
        }
        return null;
    }

    @Override
    public AuditCommentRS auditComment(AuditCommentVO request) {
        AuditCommentRS response = new AuditCommentRS();
        try {
            request.requestCheck();
            String commentId = request.getCommentId();
            Comment comment = commentMapper.selectByPrimaryKey(commentId);
            if (comment == null || !comment.getIsDeleted()) {
                throw new CommentException(CommentRetCode.CURRENT_COMMENT_NOT_EXIST.getCode(), CommentRetCode.CURRENT_COMMENT_NOT_EXIST.getMessage());
            }
            comment.setIsValid(request.isValid());
            comment.setValidationUserId(request.getValidationUserId());
            comment.setValidationTime(new Date());
            comment.setValidationSuggestion(request.getValidationSuggestion());
            commentMapper.updateByPrimaryKey(comment);

            response.setCode(CommentRetCode.SUCCESS.getCode());
            response.setMsg(CommentRetCode.SUCCESS.getMessage());
        } catch (Exception e) {
            ExceptionProcessorUtil.handleException(response, e);
        }
        return response;
    }

    @Override
    public ItemScoreRS itemScore(ItemScoreVO request) {
        ItemScoreRS response = new ItemScoreRS();
        try {
            request.requestCheck();
            String itemId = request.getItemId();
            Example example = new Example(Comment.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("itemId", itemId);
            criteria.andEqualTo("type", 1);
            int goodCommentCount = commentMapper.selectCountByExample(example);

            example = new Example(Comment.class);
            criteria = example.createCriteria();
            criteria.andEqualTo("itemId", itemId);
            criteria.andEqualTo("type", 3);
            int badCommentCount = commentMapper.selectCountByExample(example);
            if (badCommentCount == 0) {
                response.setScore(100);
            } else {
                double score = goodCommentCount / (goodCommentCount + badCommentCount);
                response.setScore(score);
            }
            response.setCode(CommentRetCode.SUCCESS.getCode());
            response.setMsg(CommentRetCode.SUCCESS.getMessage());
        } catch (Exception e) {
            ExceptionProcessorUtil.handleException(response, e);
        }
        return response;
    }

    /**
     * 执行业务逻辑
     *
     * @param request 评价参数
     */
    private AddCommentRS doAddComment(AddCommentVO request) {
        AddCommentRS response = new AddCommentRS();
        String orderItemId = request.getOrderItemId();

        OrderItemVO orderItemRequest = new OrderItemVO();
        orderItemRequest.setOrderItemId(orderItemId);
        OrderItemRS orderItemResponse = orderQueryService.orderItem(orderItemRequest);
        OrderDto orderDto = orderItemResponse.getOrderDto();
        String orderId = orderItemResponse.getOrderId();
        String itemId = orderItemResponse.getItemId();

        Example example = new Example(Comment.class);
        example.createCriteria()
                .andEqualTo(orderId)
                .andEqualTo(itemId);
        List<Comment> comments = commentMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(comments)) {
            throw new CommentException(CommentRetCode.CURRENT_ORDER_ITEM_EXISTS_COMMENT.getCode(), CommentRetCode.CURRENT_ORDER_ITEM_EXISTS_COMMENT.getMessage());
        }

        Comment comment = new Comment();
        comment.setId(globalIdGeneratorUtil.getNextSeq(COMMENT_GLOBAL_ID_CACHE_KEY, 1));
        comment.setOrderId(orderId);
        comment.setItemId(itemId);
        if (request.getStar() == null) {
            comment.setStar((byte) 5);
        } else {
            comment.setStar(request.getStar().byteValue());
        }
        if (request.getType() == null) {
            comment.setType((byte) 1);
        } else {
            comment.setType(request.getType().byteValue());
        }
        if (request.getIsAnoymous() == null) {
            comment.setIsAnoymous(true);
        } else {
            comment.setIsAnoymous(request.getIsAnoymous());
        }
        comment.setContent(request.getContent());
        comment.setBuyerNick(orderDto.getBuyerNick());
        comment.setCommentTime(new Date());
        comment.setIsPublic(request.getIsPublic());
        comment.setIsValid(false);
        comment.setIsTop(false);
        comment.setIsDeleted(false);
        comment.setUserId(orderDto.getUserId());
        commentMapper.insert(comment);

        if (CollectionUtils.isEmpty(request.getPicPaths())) {
            saveCommentPictures(comment.getId(), request.getPicPaths());
        }
        response.setCode(CommentRetCode.SUCCESS.getCode());
        response.setMsg(CommentRetCode.SUCCESS.getMessage());
        return response;
    }

    /**
     * 保存评价图片
     *
     * @param commentId 商品评价id
     * @param picPaths  图片路径
     */
    private void saveCommentPictures(String commentId, List<String> picPaths) {
        CommentPicture commentPicture;
        for (String picPath : picPaths) {
            commentPicture = new CommentPicture();
            commentPicture.setCommentId(commentId);
            commentPicture.setPicPath(picPath);
            commentPicture.setId(globalIdGeneratorUtil.getNextSeq(COMMENT_PICTURE_GLOBAL_ID_CACHE_KEY, 1));
            commentPictureMapper.insert(commentPicture);
        }
    }

    private void checkSensitiveWords(String content) {
        boolean exist = SensitiveWordsUtil.existSensitiveWords(content);
        if (exist) {
            throw new CommentException(CommentRetCode.EXIST_SENSITIVE_WORDS.getCode(), CommentRetCode.EXIST_SENSITIVE_WORDS.getMessage());
        }
    }
}
