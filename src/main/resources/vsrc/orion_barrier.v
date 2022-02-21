module orion_barrier (
  input  wire start,
  input  wire in_req,
  output wire in_ack,
  output wire out_req,
  input  wire out_ack
);

assign in_ack = out_ack;
assign out_req = start & in_req;

endmodule
