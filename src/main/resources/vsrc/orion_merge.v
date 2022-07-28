`timescale 1ns/1ps

module orion_merge #(
  parameter WIDTH   = 1,
  parameter PA_INIT = 0,
  parameter PB_INIT = 0,
  parameter PC_INIT = 0
)(
  input  wire             reset
  
  input  wire             inA_req,
  output wire             inA_ack,
  input  wire [WIDTH-1:0] inA_data,
  
  input  wire             inB_req,
  output wire             inB_ack,
  input  wire [WIDTH-1:0] inB_data,
    
  output wire             out_req,
  input  wire             out_ack,
  output wire [WIDTH-1:0] out_data

);

wire inA_token;
wire inB_token;
wire out_bubble;
wire click;

reg  pa;
reg  pb;
reg  pc;

assign inA_token    = inA_req ^ pa;
assign inB_token    = inB_req ^ pb;
assign out_bubble   = ~(pc ^ out_ack);

assign #10ps click  = inA_token | inB_token;

always @(posedge click or posedge reset) begin
  if(reset) begin
    pc  <= PC_INIT;
  end else begin
    pc  <= #10ps ~pc;
  end
end

always @(posedge out_bubble or posedge reset) begin
  if(reset) begin
    pa  <= PA_INIT;
    pb  <= PB_INIT;
  end else begin
    pa  <= #10ps inA_req;
    pb  <= #10ps inB_req;
  end
end

assign out_data = inA_token ? inA_data : inB_token ? inB_data : {{WIDTH}{1'b0}};

assign out_req  = pc;
assign inA_ack  = pa;
assign inB_ack  = pb;


endmodule
